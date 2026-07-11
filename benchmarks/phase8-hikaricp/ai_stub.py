# -*- coding: utf-8 -*-
"""느린 AI 엔진 스텁 — Phase 8 before/after 재현용.

/analyze 요청을 받으면 DELAY초 동안 응답을 지연시킨 뒤 202 형식 JSON을 돌려준다.
(옛 Flask 동기 시절의 '느린 /analyze'를 흉내 — before 코드에서는 이 대기 내내
 DB 커넥션이 트랜잭션에 붙잡혀 있게 된다)
"""
import json
import os
import sys
import time
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DELAY = float(os.environ.get("AI_STUB_DELAY", "10"))
PORT = int(os.environ.get("AI_STUB_PORT", "5000"))

_counter = 0
_lock = threading.Lock()


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_POST(self):
        global _counter
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length) if length else b"{}"
        try:
            req = json.loads(body.decode("utf-8"))
        except Exception:
            req = {}

        with _lock:
            _counter += 1
            n = _counter

        time.sleep(DELAY)  # 느린 AI 응답 재현 (핵심)

        resp = json.dumps({
            "task_id": f"stub-{n}",
            "article_id": req.get("article_id"),
            "status": "PENDING",
            "message": "stub accepted",
        }).encode("utf-8")
        self.send_response(202)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(resp)))
        self.end_headers()
        self.wfile.write(resp)

    def log_message(self, fmt, *args):
        pass  # 요청 로그 소음 제거


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"[ai-stub] port={PORT} delay={DELAY}s", flush=True)
    server.serve_forever()
