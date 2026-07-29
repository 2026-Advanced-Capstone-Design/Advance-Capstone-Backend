# -*- coding: utf-8 -*-
"""HikariCP 지표 폴러 — /actuator/prometheus를 1초 간격으로 읽어 CSV로 기록."""
import csv
import re
import sys
import time
import urllib.request

URL = "http://localhost:8081/actuator/prometheus"
OUT = sys.argv[1] if len(sys.argv) > 1 else "hikari.csv"
DURATION = float(sys.argv[2]) if len(sys.argv) > 2 else 240

PATTERNS = {
    "active": re.compile(r'^hikaricp_connections_active\{[^}]*\}\s+([0-9.]+)', re.M),
    "pending": re.compile(r'^hikaricp_connections_pending\{[^}]*\}\s+([0-9.]+)', re.M),
    "idle": re.compile(r'^hikaricp_connections_idle\{[^}]*\}\s+([0-9.]+)', re.M),
    "max": re.compile(r'^hikaricp_connections_max\{[^}]*\}\s+([0-9.]+)', re.M),
}

start = time.time()
with open(OUT, "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["elapsed_s", "active", "pending", "idle", "max"])
    while time.time() - start < DURATION:
        t = round(time.time() - start, 1)
        try:
            text = urllib.request.urlopen(URL, timeout=5).read().decode("utf-8")
            row = [t]
            for key in ("active", "pending", "idle", "max"):
                m = PATTERNS[key].search(text)
                row.append(float(m.group(1)) if m else "")
            w.writerow(row)
            f.flush()
        except Exception as e:
            w.writerow([t, "", "", "", ""])
            f.flush()
        time.sleep(1)
print(f"[poller] done -> {OUT}", flush=True)
