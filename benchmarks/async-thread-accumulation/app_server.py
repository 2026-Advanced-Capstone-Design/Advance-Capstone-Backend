# -*- coding: utf-8 -*-
"""동기 vs 비동기 요청 처리 재현 서버 — 트러블 1 (동기 대기·스레드 누적) before/after.

프로덕션 백엔드의 두 설계를 최소 재현한다.
  MODE=sync  (before): 요청 스레드가 AI 호출(DELAY초) 응답까지 **직접 블로킹**.
             고정 크기 요청 스레드풀(REQ_POOL, = Tomcat maxThreads 축소판)을 세마포어로 모사 →
             동시 요청이 풀을 넘으면 스레드가 상한까지 점유되고 나머지는 대기(큐).
  MODE=async (after):  요청 스레드는 작업을 aiWorkerExecutor(WORKER_MAX)로 넘기고 **즉시 202 반환**.
             요청 스레드는 곧바로 반납 → busy_request_threads ≈ 0.

지표(GET /metrics, Prometheus 텍스트 형식):
  busy_request_threads   현재 AI 응답을 동기 대기 중인 요청 스레드 수  ← "스레드 누적"의 핵심
  waiting_request_threads 요청 스레드풀 자리를 기다리는(큐) 스레드 수
  worker_active_threads   비동기 워커풀에서 처리 중인 작업 수 (async 전용)
  worker_queued_tasks     비동기 워커풀 큐 대기 작업 수 (async 전용)
  accepted_total          누적 접수 요청 수

실행:
  MODE=sync  PORT=8090 python app_server.py
  MODE=async PORT=8090 python app_server.py
환경변수: MODE, PORT, AI_DELAY(기본5), REQ_POOL(기본20), WORKER_MAX(기본8)
"""
import os, time, json, threading
from concurrent.futures import ThreadPoolExecutor
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

MODE = os.environ.get("MODE", "sync").lower()
PORT = int(os.environ.get("PORT", "8090"))
AI_DELAY = float(os.environ.get("AI_DELAY", "5"))
REQ_POOL = int(os.environ.get("REQ_POOL", "20"))     # 동기 요청 스레드풀 상한 (Tomcat maxThreads 축소판)
WORKER_MAX = int(os.environ.get("WORKER_MAX", "8"))  # aiWorkerExecutor 최대 스레드

# ── 계측 상태 ──
_lock = threading.Lock()
busy_req = 0        # AI 응답 동기 대기 중인 요청 스레드
waiting_req = 0     # 요청 스레드풀 자리 대기 중
accepted = 0

# 동기: 고정 크기 요청 스레드풀을 세마포어로 모사
req_slots = threading.Semaphore(REQ_POOL)

# 비동기: 워커풀 + 큐 계측
_worker = ThreadPoolExecutor(max_workers=WORKER_MAX) if MODE == "async" else None
w_submitted = w_started = w_done = 0


def _ai_call():
    """AI 엔진 HTTP 호출(3~10초)을 모사 — DELAY초 블로킹."""
    time.sleep(AI_DELAY)


def _worker_task():
    global w_started, w_done
    with _lock:
        w_started += 1
    _ai_call()
    with _lock:
        w_done += 1


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def _send(self, code, obj):
        body = json.dumps(obj).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path.startswith("/metrics"):
            with _lock:
                wa = (w_started - w_done) if MODE == "async" else 0
                wq = (w_submitted - w_started) if MODE == "async" else 0
                text = (
                    f"busy_request_threads {busy_req}\n"
                    f"waiting_request_threads {waiting_req}\n"
                    f"worker_active_threads {wa}\n"
                    f"worker_queued_tasks {wq}\n"
                    f"accepted_total {accepted}\n"
                )
            b = text.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/plain")
            self.send_header("Content-Length", str(len(b)))
            self.end_headers()
            self.wfile.write(b)
        else:
            self._send(404, {"error": "not found"})

    def do_POST(self):
        global busy_req, waiting_req, accepted, w_submitted
        length = int(self.headers.get("Content-Length", 0))
        if length:
            self.rfile.read(length)
        with _lock:
            accepted += 1

        if MODE == "sync":
            # before: 요청 스레드가 AI 응답까지 직접 대기 (풀 자리 잡고 블로킹)
            with _lock:
                waiting_req += 1
            req_slots.acquire()          # Tomcat 요청 스레드 확보 대기 (풀 포화 시 큐잉)
            with _lock:
                waiting_req -= 1
                busy_req += 1
            try:
                _ai_call()               # ← 응답까지 스레드 점유 (누적의 원인)
                self._send(200, {"status": "DONE", "mode": "sync"})
            finally:
                with _lock:
                    busy_req -= 1
                req_slots.release()
        else:
            # after: 워커풀로 위임하고 즉시 반환 → 요청 스레드 곧바로 반납
            with _lock:
                w_submitted += 1
            _worker.submit(_worker_task)
            self._send(202, {"status": "PENDING", "mode": "async"})

    def log_message(self, *a):
        pass


class Server(ThreadingHTTPServer):
    request_queue_size = 256   # listen backlog — 동시 연결 60건이 OS 큐에서 대기하지 않도록
    daemon_threads = True
    allow_reuse_address = True


if __name__ == "__main__":
    srv = Server(("0.0.0.0", PORT), Handler)
    print(f"[app] MODE={MODE} PORT={PORT} AI_DELAY={AI_DELAY}s REQ_POOL={REQ_POOL} WORKER_MAX={WORKER_MAX}", flush=True)
    srv.serve_forever()
