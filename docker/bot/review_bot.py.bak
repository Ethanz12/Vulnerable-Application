#!/usr/bin/env python3
"""Admin review bot for the Campus Events training lab.

Runs inside the isolated lab (docker compose service `bot`). It exposes a
small HTTP API; the webapp calls POST /review whenever an organiser submits
an event for review. For each job the bot signs in through the real login
form with the seeded administrator account, opens the admin review page for
the event, lets it fully render (including any stored content), and saves a
full-page screenshot. It is intentionally a real browser on the real pages -
no API shortcuts - so stored payloads execute exactly as they would for an
administrator.

Endpoints (port 5000):
    GET  /health   -> {"ok": true}
    GET  /status   -> queue depth + recent job history
    POST /review   -> {"event_id": 2}  enqueue a review job (requires X-Bot-Token)
    POST /reset    -> drop queued jobs and clear history (requires X-Bot-Token)
"""

import json
import os
import queue
import sys
import threading
import time
import secrets
import uuid
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from playwright.sync_api import sync_playwright

APP_URL = os.environ.get("APP_URL", "http://web:8080/campus-events").rstrip("/")
ADMIN_USER = os.environ.get("ADMIN_USER", "admin")
ADMIN_PASS = os.environ.get("ADMIN_PASS", "Admin#2026!")
SCREENSHOT_DIR = os.environ.get("SCREENSHOT_DIR", "/bot/screenshots")
RENDER_WAIT_MS = int(os.environ.get("RENDER_WAIT_MS", "2500"))
MAX_ATTEMPTS = int(os.environ.get("MAX_ATTEMPTS", "3"))

BOT_TOKEN = secrets.token_hex(32)

JOBS = queue.Queue()
HISTORY = []
HISTORY_LOCK = threading.Lock()


def log(msg):
    print(f"[bot] {datetime.now(timezone.utc).isoformat()} {msg}", flush=True)


def record(job, **fields):
    entry = {
        "id": job["id"],
        "kind": job["kind"],
        "queued_at": job["queued_at"],
        **fields,
    }
    with HISTORY_LOCK:
        HISTORY.append(entry)
        del HISTORY[:-50]


def screenshot_path(prefix):
    os.makedirs(SCREENSHOT_DIR, exist_ok=True)
    ts = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    return os.path.join(SCREENSHOT_DIR, f"{prefix}-{ts}.png")


def run_review(event_id):
    """Sign in as admin, open review page for event_id, screenshot it."""
    with sync_playwright() as p:
        browser = p.chromium.launch()
        try:
            page = browser.new_page(viewport={"width": 1440, "height": 1000})

            page.goto(f"{APP_URL}/login.jsp", wait_until="load", timeout=30000)
            page.wait_for_selector("#username", timeout=30000)
            page.fill("#username", ADMIN_USER)
            page.fill("#password", ADMIN_PASS)
            page.click("#login-btn")
            page.wait_for_url("**/admin.jsp", timeout=30000)
            log("logged in as admin")

            page.goto(f"{APP_URL}/review.jsp?id={event_id}",
                      wait_until="load", timeout=30000)
            page.wait_for_selector("#review-desc", timeout=30000)
            page.wait_for_timeout(RENDER_WAIT_MS)

            path = screenshot_path(f"review-{event_id}")
            page.screenshot(path=path, full_page=True)
            log(f"screenshot saved: {path}")
            return {"screenshot": path}
        finally:
            browser.close()




def worker():
    while True:
        job = JOBS.get()
        try:
            result = None
            last_err = None
            for attempt in range(1, MAX_ATTEMPTS + 1):
                try:
                    if job["kind"] == "review":
                        result = run_review(job["event_id"])
                    break
                except Exception as exc:  # noqa: BLE001 - keep the bot alive
                    last_err = f"{type(exc).__name__}: {exc}"
                    log(f"job {job['id']} attempt {attempt}/{MAX_ATTEMPTS} "
                        f"failed: {last_err}")
                    time.sleep(2 * attempt)
            if result is not None:
                record(job, status="done", attempts=attempt, **result)
            else:
                record(job, status="error", attempts=MAX_ATTEMPTS,
                       error=last_err)
        finally:
            JOBS.task_done()


class Handler(BaseHTTPRequestHandler):
    server_version = "Python/3.x"

    def _send(self, code, payload):
        body = json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _body(self):
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length) if length else b"{}"
        return json.loads(raw.decode() or "{}")

    def _check_auth(self):
        token = self.headers.get("X-Bot-Token")
        if token != BOT_TOKEN:
            self._send(401, {"ok": False, "error": "unauthorized"})
            return False
        return True

    def do_GET(self):
        if self.path == "/health":
            self._send(200, {"ok": True})
        elif self.path == "/status":
            with HISTORY_LOCK:
                history = list(HISTORY[-20:])
            self._send(200, {
                "queued": JOBS.qsize(),
                "history": history,
            })
        else:
            self._send(404, {"ok": False, "error": "not found"})

    def do_POST(self):
        if not self._check_auth():
            return
        if self.path == "/review":
            try:
                event_id = int(self._body()["event_id"])
            except Exception:  # noqa: BLE001
                self._send(400, {"ok": False,
                                 "error": "body must be {\"event_id\": <int>}"})
                return
            job = {"id": uuid.uuid4().hex[:8], "kind": "review",
                   "event_id": event_id,
                   "queued_at": datetime.now(timezone.utc).isoformat()}
            JOBS.put(job)
            log(f"queued review of event {event_id} as job {job['id']}")
            self._send(202, {"ok": True, "job": job["id"]})
        elif self.path == "/reset":
            drained = 0
            while True:
                try:
                    JOBS.get_nowait()
                    JOBS.task_done()
                    drained += 1
                except queue.Empty:
                    break
            with HISTORY_LOCK:
                HISTORY.clear()
            log(f"reset: dropped {drained} queued job(s), history cleared")
            self._send(200, {"ok": True, "dropped": drained})
        else:
            self._send(404, {"ok": False, "error": "not found"})

    def log_message(self, fmt, *args):
        log(f"{self.address_string()} {fmt % args}")


def main():
    os.makedirs(SCREENSHOT_DIR, exist_ok=True)
    threading.Thread(target=worker, daemon=True).start()
    port = int(os.environ.get("BOT_PORT", "5000"))
    server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    log(f"review bot up on :{port} (app={APP_URL}, token={BOT_TOKEN})")
    server.serve_forever()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(0)
