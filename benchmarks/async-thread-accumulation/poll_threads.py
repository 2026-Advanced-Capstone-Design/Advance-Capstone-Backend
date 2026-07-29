# -*- coding: utf-8 -*-
"""스레드 점유 지표 폴러 — 1초 간격으로 /metrics(또는 /actuator/prometheus)를 읽어 CSV로 기록.

재현 서버(app_server.py):   URL=http://localhost:8090/metrics
실제 백엔드(선택):          URL=http://<host>:8080/actuator/prometheus
  이 경우 busy_request_threads 대신 tomcat_threads_busy_threads 를,
  worker_* 대신 executor_active_threads / executor_queued_tasks 를 잡도록 --real 플래그 사용.

사용법:
  python poll_threads.py <out_csv> <duration_s> [url] [--real]
"""
import csv, re, sys, time, urllib.request

OUT = sys.argv[1] if len(sys.argv) > 1 else "threads.csv"
DURATION = float(sys.argv[2]) if len(sys.argv) > 2 else 60
URL = sys.argv[3] if len(sys.argv) > 3 and not sys.argv[3].startswith("--") else "http://localhost:8090/metrics"
REAL = "--real" in sys.argv

if REAL:
    PATTERNS = {
        "busy":    re.compile(r'^tomcat_threads_busy_threads\{[^}]*\}\s+([0-9.]+)', re.M),
        "waiting": re.compile(r'^jvm_threads_states_threads\{[^}]*state="timed-waiting"[^}]*\}\s+([0-9.]+)', re.M),
        "w_active":re.compile(r'^executor_active_threads\{[^}]*name="aiWorkerExecutor"[^}]*\}\s+([0-9.]+)', re.M),
        "w_queued":re.compile(r'^executor_queued_tasks\{[^}]*name="aiWorkerExecutor"[^}]*\}\s+([0-9.]+)', re.M),
    }
else:
    PATTERNS = {
        "busy":    re.compile(r'^busy_request_threads\s+([0-9.]+)', re.M),
        "waiting": re.compile(r'^waiting_request_threads\s+([0-9.]+)', re.M),
        "w_active":re.compile(r'^worker_active_threads\s+([0-9.]+)', re.M),
        "w_queued":re.compile(r'^worker_queued_tasks\s+([0-9.]+)', re.M),
    }

start = time.time()
with open(OUT, "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["elapsed_s", "busy", "waiting", "w_active", "w_queued"])
    while time.time() - start < DURATION:
        t = round(time.time() - start, 1)
        try:
            text = urllib.request.urlopen(URL, timeout=5).read().decode("utf-8")
            row = [t]
            for key in ("busy", "waiting", "w_active", "w_queued"):
                m = PATTERNS[key].search(text)
                row.append(float(m.group(1)) if m else "")
            w.writerow(row); f.flush()
        except Exception:
            w.writerow([t, "", "", "", ""]); f.flush()
        time.sleep(1)
print(f"[poller] done -> {OUT}", flush=True)
