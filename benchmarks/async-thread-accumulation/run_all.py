# -*- coding: utf-8 -*-
"""원샷 오케스트레이터 — sync/async 두 모드를 각각 기동·부하·계측하고 차트까지 생성.

    python run_all.py

각 모드마다: app_server 기동 → 폴러(백그라운드) → 부하 → 드레인 대기 → 서버 종료.
결과: data/threads_sync.csv, data/threads_async.csv, data/load_*.csv, *-before-after.png
"""
import os, sys, time, subprocess, threading, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "data")
os.makedirs(DATA, exist_ok=True)
PY = sys.executable
PORT = "8090"
AI_DELAY = os.environ.get("AI_DELAY", "5")
REQ_POOL = os.environ.get("REQ_POOL", "20")
WORKER_MAX = os.environ.get("WORKER_MAX", "8")
BURST = os.environ.get("BURST", "60")
POLL_S = float(os.environ.get("POLL_S", "45"))


def wait_up(url, tries=50):
    for _ in range(tries):
        try:
            urllib.request.urlopen(url, timeout=1).read(); return True
        except Exception:
            time.sleep(0.2)
    return False


def run_mode(mode):
    print(f"\n===== MODE={mode} =====", flush=True)
    env = dict(os.environ, MODE=mode, PORT=PORT, AI_DELAY=AI_DELAY, REQ_POOL=REQ_POOL, WORKER_MAX=WORKER_MAX)
    srv = subprocess.Popen([PY, os.path.join(HERE, "app_server.py")], env=env)
    try:
        if not wait_up(f"http://localhost:{PORT}/metrics"):
            raise RuntimeError("server not up")
        threads_csv = os.path.join(DATA, f"threads_{mode}.csv")
        load_csv = os.path.join(DATA, f"load_{mode}.csv")
        poller = subprocess.Popen([PY, os.path.join(HERE, "poll_threads.py"), threads_csv, str(POLL_S),
                                   f"http://localhost:{PORT}/metrics"])
        time.sleep(2)  # 베이스라인 몇 초
        subprocess.call([PY, os.path.join(HERE, "load.py"), BURST, f"http://localhost:{PORT}/submit", load_csv])
        poller.wait()
    finally:
        srv.terminate()
        try: srv.wait(timeout=5)
        except Exception: srv.kill()


if __name__ == "__main__":
    run_mode("sync")
    run_mode("async")
    env = dict(os.environ, REQ_POOL=REQ_POOL)
    subprocess.call([PY, os.path.join(HERE, "make_chart.py")], env=env)          # 스레드 점유
    subprocess.call([PY, os.path.join(HERE, "make_latency_charts.py")], env=env) # 응답시간(+단독 2장)
    print("\n[done] 차트 생성 완료 → async-thread-accumulation-before-after.png, "
          "async-latency-before-after.png (+ before/after 단독)", flush=True)
