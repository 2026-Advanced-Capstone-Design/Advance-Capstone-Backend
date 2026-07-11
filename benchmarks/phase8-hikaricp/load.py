# -*- coding: utf-8 -*-
"""Phase 8 재현 부하 생성기 — 2단계.

1단계(제출 버스트): /analyze/text 108건 동시 제출.
   before 코드: aiWorkerExecutor(최대 8스레드)가 느린 AI 응답(10s)을 기다리는 내내
   @Transactional 때문에 DB 커넥션 8개를 점유 → 풀(10)에 2개만 남음.
   108 = 워커 8 + 큐 100 (거절 예외 없이 큐를 가득 채워 스레드를 8까지 성장시키는 크기)

2단계(조회 폴링): 프론트의 상태 폴링을 흉내 — 30개 스레드가 90초간
   GET /{id}/status 반복. before에선 남은 커넥션 2개를 두고 경합
   → hikaricp pending 급증 + 응답 지연. after에선 풀이 비어 있어 빠름.

사용법: python load.py [burst_n] [poll_threads] [poll_seconds] [out_csv]
"""
import concurrent.futures
import csv
import json
import random
import sys
import threading
import time
import urllib.request
import urllib.error

BASE = "http://localhost:8081/api/v1/articles"
BURST_N = int(sys.argv[1]) if len(sys.argv) > 1 else 108
POLL_THREADS = int(sys.argv[2]) if len(sys.argv) > 2 else 30
POLL_SECONDS = float(sys.argv[3]) if len(sys.argv) > 3 else 90
OUT = sys.argv[4] if len(sys.argv) > 4 else "load_result.csv"

BASE_TEXT = (
    "정부는 오늘 새로운 경제 정책을 발표했다. 이번 정책은 중소기업 지원과 "
    "일자리 창출에 초점을 맞추고 있으며, 전문가들은 엇갈린 평가를 내놓고 있다. "
    "일부는 실효성에 의문을 제기했고, 다른 일부는 시의적절한 조치라고 평가했다. "
    "부하 테스트용 본문입니다. "
)

rows = []
rows_lock = threading.Lock()


def record(phase, code, ms):
    with rows_lock:
        rows.append([round(time.time() - T0, 1), phase, code, ms])


def submit(i):
    payload = json.dumps({"text": f"{BASE_TEXT} (요청 번호 {i}, ts={time.time()})"}).encode("utf-8")
    req = urllib.request.Request(
        f"{BASE}/analyze/text", data=payload,
        headers={"Content-Type": "application/json"}, method="POST"
    )
    t0 = time.time()
    article_id = None
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            code = r.status
            try:
                data = json.loads(r.read().decode("utf-8"))
                d = data.get("data") or {}
                article_id = d.get("articleId") or d.get("article_id") or d.get("id")
            except Exception:
                pass
    except urllib.error.HTTPError as e:
        code = e.code
    except Exception:
        code = -1
    record("submit", code, round((time.time() - t0) * 1000))
    return article_id


def poll_worker(ids, stop_at):
    while time.time() < stop_at:
        aid = random.choice(ids)
        t0 = time.time()
        try:
            with urllib.request.urlopen(f"{BASE}/{aid}/status", timeout=60) as r:
                code = r.status
        except urllib.error.HTTPError as e:
            code = e.code
        except Exception:
            code = -1
        record("poll", code, round((time.time() - t0) * 1000))
        time.sleep(0.1)


def pct(sorted_list, p):
    if not sorted_list:
        return -1
    return sorted_list[max(0, int(len(sorted_list) * p) - 1)]


if __name__ == "__main__":
    T0 = time.time()
    print(f"[load] 1단계: {BURST_N}건 동시 제출", flush=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=BURST_N) as ex:
        ids = [a for a in ex.map(submit, range(BURST_N)) if a]
    if not ids:
        ids = list(range(1, BURST_N + 1))  # 응답 파싱 실패 시 ddl-auto=create 기준 추정
    print(f"[load] 제출 완료 ({round(time.time()-T0,1)}s), article ids {len(ids)}개", flush=True)

    print(f"[load] 2단계: {POLL_THREADS}스레드 x {POLL_SECONDS}s 상태 폴링", flush=True)
    stop_at = time.time() + POLL_SECONDS
    threads = [threading.Thread(target=poll_worker, args=(ids, stop_at)) for _ in range(POLL_THREADS)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    with open(OUT, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["t_s", "phase", "status", "elapsed_ms"])
        w.writerows(rows)

    for phase in ("submit", "poll"):
        sub = [r for r in rows if r[1] == phase]
        codes = {}
        for r in sub:
            codes[r[2]] = codes.get(r[2], 0) + 1
        lat = sorted(r[3] for r in sub if r[2] == 200)
        print(f"[load] {phase}: n={len(sub)} codes={codes} "
              f"avg={round(sum(lat)/len(lat)) if lat else -1}ms "
              f"p95={pct(lat, 0.95)}ms p99={pct(lat, 0.99)}ms", flush=True)
