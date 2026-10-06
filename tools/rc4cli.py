#!/usr/bin/env python3
"""RC4 envelope helper for the Campus Events training lab.

The app wraps every request/response body in {"enc": base64(rc4(static_key, body))}.
This tool encrypts/decrypts envelopes offline and can also drive full HTTP
round trips against the running lab, decrypting responses for you.

Examples:
    echo '{"action":"login","username":"admin","password":"Admin#2026!"}' \
        | python3 tools/rc4cli.py encrypt

    python3 tools/rc4cli.py request --jar /tmp/admin.jar \
        --data '{"action":"login","username":"admin","password":"Admin#2026!"}' \
        api/auth

    python3 tools/rc4cli.py request --jar /tmp/admin.jar api/admin/users

    curl -s .../api/directory/pending | python3 tools/rc4cli.py decrypt

Options:
    --base URL    app base URL (default http://localhost:8080/campus-events,
                  env APP_URL)
    --key KEY     RC4 key (default Rc4StaticKey#Campus2026, env RC4_KEY)
    --jar PATH    cookie jar file; loads/saves JSESSIONID between calls so
                  consecutive requests share one session
    --data JSON   request body (plain JSON; tool encrypts it)
    --raw         print the raw HTTP body instead of decrypting
"""

import argparse
import base64
import http.cookiejar
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

DEFAULT_BASE = "http://localhost:8080/campus-events"
DEFAULT_KEY = "Rc4StaticKey#Campus2026"


def rc4(key: bytes, data: bytes) -> bytes:
    s = list(range(256))
    j = 0
    for i in range(256):
        j = (j + s[i] + key[i % len(key)]) & 0xFF
        s[i], s[j] = s[j], s[i]
    out = bytearray()
    i = j = 0
    for c in data:
        i = (i + 1) & 0xFF
        j = (j + s[i]) & 0xFF
        s[i], s[j] = s[j], s[i]
        out.append(c ^ s[(s[i] + s[j]) & 0xFF])
    return bytes(out)


def encrypt(key: str, plaintext: str) -> str:
    return base64.b64encode(rc4(key.encode(), plaintext.encode())).decode()


def decrypt(key: str, envelope: str) -> str:
    envelope = envelope.strip()
    if envelope.startswith('{"enc"'):
        envelope = json.loads(envelope)["enc"]
    return rc4(key.encode(), base64.b64decode(envelope)).decode(
        "utf-8", "replace")


def load_jar(path):
    jar = http.cookiejar.MozillaCookieJar()
    if path and os.path.exists(path):
        jar.load(path, ignore_discard=True, ignore_expires=True)
    return jar


def save_jar(jar, path):
    if path:
        jar.save(path, ignore_discard=True, ignore_expires=True)


def do_request(args, jar, path, body_plain=None, method=None):
    url = args.base.rstrip("/") + "/" + path.lstrip("/")
    data = None
    headers = {}
    if body_plain is not None:
        envelope = json.dumps({"enc": encrypt(args.key, body_plain)})
        data = envelope.encode()
        headers["Content-Type"] = "application/json"
    opener = urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(jar))
    req = urllib.request.Request(url, data=data, headers=headers,
                                 method=method or ("POST" if data else "GET"))
    try:
        resp = opener.open(req, timeout=30)
        raw = resp.read().decode("utf-8", "replace")
        status = resp.status
        ctype = resp.headers.get("Content-Type", "")
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        status = e.code
        ctype = e.headers.get("Content-Type", "")
    save_jar(jar, args.jar)
    if args.raw:
        print(raw)
    else:
        text = raw
        if "json" in ctype or raw.lstrip().startswith('{"enc"'):
            try:
                text = decrypt(args.key, raw)
            except Exception:
                pass
        print(f"HTTP {status} {ctype}", file=sys.stderr)
        print(text)
    return 0 if status < 400 else 1


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("command", choices=["encrypt", "decrypt", "request"])
    ap.add_argument("path", nargs="?", help="API path, for `request`")
    ap.add_argument("--base", default=os.environ.get("APP_URL", DEFAULT_BASE))
    ap.add_argument("--key", default=os.environ.get("RC4_KEY", DEFAULT_KEY))
    ap.add_argument("--jar", help="cookie jar file for session persistence")
    ap.add_argument("--data", help="JSON request body (plain, unencrypted)")
    ap.add_argument("--raw", action="store_true",
                    help="print raw body, skip decryption")
    args = ap.parse_intermixed_args()

    if args.command == "encrypt":
        plain = args.data or sys.stdin.read()
        print(json.dumps({"enc": encrypt(args.key, plain)}))
    elif args.command == "decrypt":
        env = args.data or sys.stdin.read()
        print(decrypt(args.key, env.strip()))
    else:
        if not args.path:
            ap.error("request needs an API path")
        jar = load_jar(args.jar)
        rc = do_request(args, jar, args.path, body_plain=args.data)
        sys.exit(rc)


if __name__ == "__main__":
    main()
