#!/usr/bin/env python3
"""Local server for the icon maker. Standard library only (Python 3.9+).

Usage: python3 tools/icon-maker/server.py [--port 8765] [--root PATH]
"""
import argparse
import json
import os
import re
import struct
import subprocess
import sys
import tempfile
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlsplit

HERE = Path(__file__).resolve().parent
MAX_BYTES = 1024 * 1024
PNG_SIG = b"\x89PNG\r\n\x1a\n"
ICON_W, ICON_H = 72, 96
TYPES = {
    ".html": "text/html; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".png": "image/png",
    ".svg": "image/svg+xml",
}
PLAIN_NAME = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]*$")

ROOT = HERE


def find_root():
    try:
        out = subprocess.run(
            ["git", "-C", str(HERE), "rev-parse", "--show-toplevel"],
            capture_output=True, text=True, check=True).stdout.strip()
        if out:
            return Path(out)
    except Exception:
        pass
    return HERE.parent.parent


def validate_png(data):
    """Return an error string, or None if data is a 72x96 PNG within limits."""
    if len(data) > MAX_BYTES:
        return "PNG is larger than 1 MB"
    if not data.startswith(PNG_SIG):
        return "Body is not a PNG (bad signature)"
    if len(data) < 33 or data[12:16] != b"IHDR":
        return "PNG has no IHDR chunk"
    w, h = struct.unpack(">II", data[16:24])
    if (w, h) != (ICON_W, ICON_H):
        return "PNG must be %dx%d, got %dx%d" % (ICON_W, ICON_H, w, h)
    return None


def archive(data):
    folder = ROOT / "plans" / "icon-maker" / "iterations"
    folder.mkdir(parents=True, exist_ok=True)
    stamp = time.strftime("%Y%m%d-%H%M%S")
    n = 1
    while True:
        name = stamp + ("" if n == 1 else "-%d" % n) + ".png"
        path = folder / name
        try:
            with open(path, "xb") as f:
                f.write(data)
            return path
        except FileExistsError:
            n += 1


def atomic_write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(dir=str(path.parent), prefix=".icon-", suffix=".tmp")
    try:
        with os.fdopen(fd, "wb") as f:
            f.write(data)
        os.chmod(tmp, 0o644)
        os.replace(tmp, path)
    except BaseException:
        try:
            os.unlink(tmp)
        except OSError:
            pass
        raise


def rel(path):
    return os.path.relpath(path, ROOT)


class Handler(BaseHTTPRequestHandler):
    server_version = "icon-maker"

    def log_message(self, fmt, *args):
        sys.stderr.write("%s %s\n" % (self.command, self.path))

    def send_bytes(self, code, body, ctype):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)

    def send_json(self, code, obj):
        self.send_bytes(code, json.dumps(obj).encode(), "application/json")

    def send_text(self, code, msg):
        self.send_bytes(code, (msg + "\n").encode(), "text/plain; charset=utf-8")

    def send_file(self, path, expected=None):
        ctype = TYPES.get(path.suffix.lower(), "application/octet-stream")
        try:
            body = path.read_bytes()
        except OSError:
            self.send_text(404, "Not found. Expected file at: %s" % (expected or path))
            return
        self.send_bytes(200, body, ctype)

    def do_GET(self):
        p = unquote(urlsplit(self.path).path)
        if p in ("/", "/index.html"):
            return self.send_file(HERE / "index.html", "tools/icon-maker/index.html")
        if p == "/assets/badge.svg":
            f = ROOT / "plans/screens/osd-icons/aspect_65_24.svg"
            return self.send_file(f, rel(f))
        if p == "/assets/sample.png":
            f = ROOT / "plans/icon-maker/field.png"
            return self.send_file(f, rel(f))
        if p == "/assets/references.json":
            folder = ROOT / "plans/screens/launcher-icons"
            items = []
            if folder.is_dir():
                for f in sorted(folder.glob("*.png")):
                    name = re.sub(r"-\d+$", "", f.stem)
                    items.append({"name": name, "url": "/assets/references/" + f.name})
            return self.send_json(200, items)
        if p.startswith("/assets/references/"):
            name = p[len("/assets/references/"):]
            if not PLAIN_NAME.match(name) or not name.endswith(".png"):
                return self.send_text(404, "Not found: bad reference name")
            f = ROOT / "plans/screens/launcher-icons" / name
            return self.send_file(f, rel(f))
        if p.startswith("/") and p.count("/") == 1:
            name = p[1:]
            if PLAIN_NAME.match(name) and Path(name).suffix.lower() in TYPES:
                return self.send_file(HERE / name, "tools/icon-maker/" + name)
        self.send_text(404, "Not found: %s" % p)

    do_HEAD = do_GET

    def read_png(self):
        try:
            length = int(self.headers.get("Content-Length", ""))
        except ValueError:
            self.send_json(400, {"error": "Content-Length required"})
            return None
        if length > MAX_BYTES:
            self.send_json(400, {"error": "PNG is larger than 1 MB"})
            self.close_connection = True
            return None
        data = self.rfile.read(length)
        err = validate_png(data)
        if err:
            self.send_json(400, {"error": err})
            return None
        return data

    def do_POST(self):
        p = urlsplit(self.path).path
        if p not in ("/api/iteration", "/api/app-icon"):
            return self.send_text(404, "Not found: %s" % p)
        data = self.read_png()
        if data is None:
            return
        try:
            archived = archive(data)
            if p == "/api/iteration":
                return self.send_json(200, {"archived": rel(archived)})
            target = ROOT / "app/src/main/res/drawable-nodpi/ic_launcher.png"
            atomic_write(target, data)
            self.send_json(200, {"saved": rel(target), "archived": rel(archived)})
        except OSError as e:
            self.send_json(500, {"error": "Write failed: %s" % e})


def main():
    global ROOT
    ap = argparse.ArgumentParser(description="Icon maker local server")
    ap.add_argument("--port", type=int, default=8765)
    ap.add_argument("--root", default=None, help="repo root (default: git top-level)")
    args = ap.parse_args()
    ROOT = Path(args.root).resolve() if args.root else find_root()
    srv = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    print("Icon maker: http://127.0.0.1:%d  (root: %s)" % (args.port, ROOT), flush=True)
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
