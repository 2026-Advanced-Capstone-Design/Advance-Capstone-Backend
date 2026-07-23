# -*- coding: utf-8 -*-
"""부하 생성기 — 분석 제출 BURST건을 동시에 쏘고 각 요청 응답시간을 기록.

before(sync): 요청 스레드가 AI 응답까지 대기 → 풀 포화 시 뒤 요청은 큐에서 대기 → 응답시간 급증.
after(async): 즉시 202 → 응답시간 수 ms.

사용법: python load.py <burst_n> <url> <out_csv>
"""
import concurrent.futures, csv, json, sys, threading, time, urllib.request, urllib.error

BURST = int(sys.argv[1]) if len(sys.argv) > 1 else 60
URL = sys.argv[2] if len(sys.argv) > 2 else "http://localhost:8090/submit"
OUT = sys.argv[3] if len(sys.argv) > 3 else "load.csv"

rows, lock, T0 = [], threading.Lock(), None


def submit(i):
    payload = json.dumps({"text": f"부하 요청 {i} ts={time.time()}"}).encode("utf-8")
    req = urllib.request.Request(URL, data=payload,
                                 headers={"Content-Type": "application/json"}, method="POST")
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            code = r.status; r.read()
    except urllib.error.HTTPError as e:
        code = e.code
    except Exception:
        code = -1
    with lock:
        rows.append([round(t0 - T0, 2), round((time.time() - t0) * 1000), code])


def pct(xs, p):
    return sorted(xs)[max(0, int(len(xs) * p) - 1)] if xs else -1


if __name__ == "__main__":
    T0 = time.time()
    print(f"[load] {BURST}건 동시 제출 → {URL}", flush=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=BURST) as ex:
        list(ex.map(submit, range(BURST)))
    with open(OUT, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f); w.writerow(["submit_t_s", "elapsed_ms", "status"]); w.writerows(rows)
    lat = [r[1] for r in rows if r[2] in (200, 202)]
    codes = {}
    for r in rows:
        codes[r[2]] = codes.get(r[2], 0) + 1
    print(f"[load] n={len(rows)} codes={codes} avg={round(sum(lat)/len(lat)) if lat else -1}ms "
          f"p95={pct(lat,0.95)}ms max={max(lat) if lat else -1}ms -> {OUT}", flush=True)
