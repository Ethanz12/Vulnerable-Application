# Campus Event & Digital Signage System — Security Training Lab

A deliberately vulnerable JSP application for **isolated security training**.
It requires a strict, sequential exploit chain:

```
Unauthenticated → Student → Organiser → Administrator → RCE
```

Every stage depends on the previous one; there is no shortcut from
unauthenticated access to admin or RCE.

> **WARNING — lab use only.** This application intentionally contains
> unauthenticated account activation flaws, IDOR, stored XSS with an automated
> admin browser bot, and unauthenticated-file-upload RCE. Run it only inside
> an isolated lab network you control. Never expose it to the internet or any
> shared network. All credentials below are synthetic training credentials.

## Stack

| Layer      | Technology                                        |
|------------|---------------------------------------------------|
| App        | Java 17, JSP/Servlets (javax), Tomcat 9           |
| Database   | PostgreSQL 16                                     |
| Rich text  | CKEditor 4 (vendored, `static/vendor/ckeditor`)   |
| Bot        | Python 3 + Playwright (Chromium), bundled service |
| Transport  | App-level RC4 envelope over plain HTTP (see below)|

## Quick start

```bash
docker compose up --build -d
docker compose ps                       # all three services should be Up
curl -s http://localhost:8080/campus-events/login.jsp   # bootstrap loader shell
```

Open `http://localhost:8080/` in a browser — a ROOT redirect page sends you
straight to the app at `http://localhost:8080/campus-events/`. Every page body
arrives encrypted and is decrypted in-browser by `static/js/app-crypto.js`;
the only plaintext HTML is a tiny bootstrap loader. **This envelope is the
vulnerability, not a defence** — it uses RC4 with a static key that is
hardcoded in `app/src/main/resources/app.properties` and shipped to every
client inside `app-crypto.js`.

### Seeded accounts

| Account       | Password      | Role       | Notes                              |
|---------------|---------------|------------|------------------------------------|
| `admin`       | `Admin#2026!` | Admin      | used by the review bot             |
| `m.organiser` | `Organiser#1!`| Organiser  | owns events 1–3, holds the invite  |
| `a.chen`      | `Student#1!`  | Student    | regular student                    |
| `r.patel`     | —             | **pending**| activation stage of the chain (V1) |

### Reset the lab

```bash
docker compose down -v        # -v wipes the seeded database volume
docker compose up --build -d
```

The bot's screenshots land in `screenshots/` (bind-mounted).

## The transport envelope (stage 0)

All request/response bodies are wrapped as:

```json
{"enc": "<base64(RC4(\"Rc4StaticKey#Campus2026\", body))>"}
```

Use the bundled helper to talk to the API from a terminal:

```bash
# login and keep the session
python3 tools/rc4cli.py request --jar /tmp/admin.jar \
    --data '{"action":"login","username":"admin","password":"Admin#2026!"}' api/auth

# authenticated call with the same jar
python3 tools/rc4cli.py request --jar /tmp/admin.jar api/admin/users
```

`rc4cli.py` also does offline work: `encrypt` / `decrypt` subcommands read
stdin (or `--data`), and the key/base URL are overridable with `--key`,
`--base`, `RC4_KEY`, `APP_URL`.

## Admin review bot (used in stage V3)

`docker/bot/review_bot.py` runs as the `bot` compose service. Whenever an organiser
submits an event for review, the webapp POSTs `{"event_id": N}` to the bot's
webhook. The bot then signs in through the real login UI with the seeded
admin account, opens `review.jsp?id=N`, lets it render fully, and saves a
full-page screenshot — so any stored payload executes in an admin session
exactly as it would against a human reviewer.

Control endpoints (port 5000, published on 127.0.0.1 only):

```bash
curl -s localhost:5000/health
curl -s localhost:5000/status                       # queue + recent results
curl -s -X POST localhost:5000/review -d '{"event_id":2}'   # manual trigger
curl -s -X POST localhost:5000/snap -d '{"url":"http://web:8080/campus-events/login.jsp"}'  # debug screenshot
curl -s -X POST localhost:5000/reset                # drop queue + history
```

The bot retries each job up to 3 times, uses a fresh browser context per job
(no stale sessions), and processes jobs sequentially. Start/stop is just the
compose service: `docker compose up -d bot` / `docker compose stop bot`.

## Documentation

- [`docs/ATTACK-CHAIN.md`](docs/ATTACK-CHAIN.md) — staged walkthrough of the
  full Unauthenticated → Student → Organiser → Administrator → RCE chain,
  with hints, payloads, and verification steps.
- `app/src/main/java/campus/` — each vulnerability is marked in-code
  (`V1` activation, `V2` team join IDOR, `V3` review sink, `V4` upload/preview).

## Component map

```
docker-compose.yml        db / web / bot services
docker/db/init/           schema + seed data (recreated on `down -v`)
app/                      Maven project, Tomcat WAR
  src/main/java/campus/   filter (envelope), servlets (V1–V4), crypto, db
  src/main/webapp/        JSPs, static assets, CKEditor vendor copy
docker/bot/               Playwright admin review bot
screenshots/              bot screenshots (bind-mounted)
tools/rc4cli.py           envelope helper for terminal-based testing
```
