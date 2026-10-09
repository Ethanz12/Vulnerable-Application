# Campus Event & Digital Signage System — Security Training Lab

## About

A deliberately vulnerable campus event management system built with Java/JSP for **isolated security training**. The application allows students to browse events, join event teams, and register attendance, while organisers can create and manage events, and administrators can review and publish them.

This lab simulates a real-world web application with multiple user roles and a complete review workflow, but contains **intentional security flaws** that create a multi-stage attack chain. The application uses a custom RC4 encryption envelope over HTTP to obscure API traffic, but this is part of the vulnerability, not a defense mechanism.

> **WARNING — lab use only.** This application intentionally contains critical security vulnerabilities including unauthenticated account activation flaws, IDOR, stored XSS with an automated admin browser bot, and remote code execution via unrestricted file upload. Run it only inside an isolated lab network you control. Never expose it to the internet or any shared network. All credentials are synthetic training credentials.

## Learning Objectives

This lab teaches the following vulnerability classes and attack techniques:

- **V1 — Unauthenticated Account Takeover**: Exploiting insecure account activation token handling to hijack pending accounts
- **V2 — Privilege Escalation via IDOR**: Using insecure direct object references to join event teams with elevated privileges
- **V3 — Stored XSS with Bot Exploitation**: Injecting persistent cross-site scripting payloads that execute in an automated admin browser session
- **V4 — Remote Code Execution via File Upload**: Achieving server-side code execution through unrestricted JSP file upload

You will practice:
- White-box source code analysis to identify vulnerability sinks
- Multi-stage exploit chain construction where each stage depends on the previous
- Session hijacking and privilege escalation techniques
- Bypassing client-side security controls (encryption envelopes, input filters)
- Interacting with automated systems (admin review bot) as attack vectors

## Prerequisites

**Required knowledge:**
- Basic understanding of web application security (OWASP Top 10)
- Familiarity with HTTP, cookies, and session management
- Command-line proficiency (bash, curl, python3)
- Basic Java/JSP reading ability (for white-box analysis)

**Required tools:**
- Docker and Docker Compose (version 20.10+)
- Python 3.8+ (for `rc4cli.py` helper)
- A web browser with developer tools
- (Optional) Burp Suite or similar intercepting proxy

**System requirements:**
- 4 GB RAM minimum (8 GB recommended)
- 10 GB free disk space
- Ports 8080 (web), 5000 (bot control) available on localhost

## Challenge Instructions

Your objective is to progress through a **strict, sequential exploit chain**:

```
Unauthenticated → Student → Organiser → Administrator → RCE
```

**There are no shortcuts.** Each stage requires compromising the previous role:

1. **Stage 1 — Unauthenticated to Student**: Find and exploit the account activation vulnerability (V1) to take over the pending `r.patel` account
2. **Stage 2 — Student to Organiser**: Use the team join IDOR (V2) to escalate your compromised student account to organiser privileges
3. **Stage 3 — Organiser to Administrator**: Craft a stored XSS payload (V3) that executes in the admin review bot's browser session and steals the admin session
4. **Stage 4 — Administrator to RCE**: With admin access, exploit the unrestricted file upload (V4) to deploy a JSP webshell and achieve remote code execution

**Hints:**
- Start by reading the source code in `app/src/main/java/campus/` — each vulnerability is marked with comments (`V1`, `V2`, `V3`, `V4`)
- The RC4 envelope is not a security boundary; the key is hardcoded and shipped to every client
- The admin review bot automatically visits events submitted for review
- All API endpoints are under `/api/*` and use the RC4 envelope

For a complete staged walkthrough with payloads and verification steps, see [`docs/ATTACK-CHAIN.md`](docs/ATTACK-CHAIN.md).

## Stack

| Layer      | Technology                                        |
|------------|---------------------------------------------------|
| App        | Java 17, JSP/Servlets (javax), Tomcat 9           |
| Database   | PostgreSQL 16                                     |
| Rich text  | CKEditor 4 (vendored, `static/vendor/ckeditor`)   |
| Bot        | Python 3 + Playwright (Chromium), bundled service |
| Transport  | App-level RC4 envelope over plain HTTP (see below)|

## Deployment Instructions

### Quick start

```bash
# Clone the repository
git clone <repository-url>
cd Vulnerable-Application

# Pre-create the bot screenshot directory (world-writable: the bot container
# writes as UID 999; if Docker creates the bind-mount source itself it is
# owned by root and the bot cannot write there)
mkdir -p screenshots && chmod 777 screenshots

# Start all services (database, web app, bot)
docker compose up --build -d

# Verify all three services are running
docker compose ps

# Test the application is responding
curl -s http://localhost:8080/campus-events/login.jsp
```

Open `http://localhost:8080/` in a browser — a ROOT redirect page sends you straight to the app at `http://localhost:8080/campus-events/`. Every page body arrives encrypted and is decrypted in-browser by `static/js/app-crypto.js`; the only plaintext HTML is a tiny bootstrap loader. **This envelope is the vulnerability, not a defence** — it uses RC4 with a static key that is hardcoded in `app/src/main/resources/app.properties` and shipped to every client inside `app-crypto.js`.

### Verify the deployment

After `docker compose up`, confirm all services are healthy:

```bash
# Check service status
docker compose ps
# Expected: db (Up), web (Up), bot (Up)

# Check web app health
curl -s http://localhost:8080/campus-events/ | head -20

# Check bot health
curl -s http://localhost:5000/health
# Expected: {"ok":true}

# Check database connectivity
docker compose exec web curl -s http://localhost:8080/campus-events/api/auth \
  -H 'Content-Type: application/json' \
  -d '{"enc":"eyJlbmMiOiJcInRlc3RcIiJ9"}'  # Will return auth error, but confirms DB is up
```

### Seeded accounts

The database is pre-populated with these accounts on first startup:

| Account       | Password      | Role       | Notes                              |
|---------------|---------------|------------|------------------------------------|
| `admin`       | `Admin#2026!` | Admin      | used by the review bot             |
| `m.organiser` | `Organiser#1!`| Organiser  | owns all seeded events, holds the invites |
| `a.chen`      | `Student#1!`  | Student    | regular student                    |
| `r.patel`     | —             | **pending**| activation stage of the chain (V1) |

### Reset the lab

To return to a clean state (wipes all data and re-seeds):

```bash
docker compose down -v        # -v wipes the seeded database volume
docker compose up --build -d
```

The bot's screenshots land in `screenshots/` (bind-mounted).

## Troubleshooting

Common issues you may encounter:

- **Services won't start**: `docker compose up` fails or services exit immediately
- **Bot returns 401 on webhook**: Web app submits event for review but bot doesn't process it
- **Can't connect to http://localhost:8080**: Browser shows "connection refused" or timeout
- **RC4 encryption errors**: API calls return garbled data or decryption fails
- **Database connection errors**: Web app logs show "Connection refused" to database
- **Bot screenshots not appearing**: `screenshots/` directory is empty after bot processes events

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

Control endpoints (port 5000, published on 127.0.0.1 only). GET endpoints are
open; POST endpoints require the shared lab token (`BOT_TOKEN` from
`docker-compose.yml`) in an `X-Bot-Token` header:

```bash
curl -s localhost:5000/health
curl -s localhost:5000/status                       # queue depth + recent job history
curl -s -X POST localhost:5000/review \
  -H 'X-Bot-Token: lab-bot-token-890d7ba8d41377626cceaa03f0ab67f1b245d8b6e57d156f66a0e7092b62f3cf' \
  -d '{"event_id":2}'                               # manual trigger
curl -s -X POST localhost:5000/reset \
  -H 'X-Bot-Token: lab-bot-token-890d7ba8d41377626cceaa03f0ab67f1b245d8b6e57d156f66a0e7092b62f3cf'  # drop queue + history
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
