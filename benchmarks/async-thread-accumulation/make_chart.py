# -*- coding: utf-8 -*-
"""트러블 1 — 동기 스레드 누적 before/after 비교 차트."""
import csv, os, sys
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "data")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "async-thread-accumulation-before-after.png")
REQ_POOL = float(os.environ.get("REQ_POOL", "20"))

# ── 팔레트 (phase8과 동일, 라이트 모드) ──
SURFACE="#fcfcfb"; INK="#0b0b0b"; INK2="#52514e"; MUTED="#898781"
GRID="#e1e0d9"; BASELINE="#c3c2b7"; C_BEFORE="#e34948"; C_AFTER="#2a78d6"

plt.rcParams["font.family"] = "Malgun Gothic"
plt.rcParams["axes.unicode_minus"] = False


def load(path):
    rows = []
    with open(path, encoding="utf-8") as f:
        for r in csv.DictReader(f):
            if r["busy"] == "":
                continue
            rows.append((float(r["elapsed_s"]), float(r["busy"]), float(r["waiting"])))
    t0 = next((t for t, b, wt in rows if b > 0), 0)
    return [(t - t0, b, wt) for t, b, wt in rows]


def load_stats(path):
    if not os.path.exists(path):
        return None
    lat = []
    with open(path, encoding="utf-8") as f:
        for r in csv.DictReader(f):
            if r["status"] in ("200", "202"):
                lat.append(float(r["elapsed_ms"]))
    if not lat:
        return None
    lat.sort()
    return {"avg": round(sum(lat)/len(lat)), "p95": lat[max(0, int(len(lat)*0.95)-1)], "max": max(lat)}


before = load(os.path.join(DATA, "threads_sync.csv"))
after = load(os.path.join(DATA, "threads_async.csv"))
sb = load_stats(os.path.join(DATA, "load_sync.csv"))
sa = load_stats(os.path.join(DATA, "load_async.csv"))

t_max = max([t for t, b, wt in before] + [t for t, b, wt in after] + [5]) + 3
T_MIN, T_MAX = -3, t_max

fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(10, 6.4), sharex=True, dpi=150)
fig.patch.set_facecolor(SURFACE)
for ax in (ax1, ax2):
    ax.set_facecolor(SURFACE)
    ax.grid(True, axis="y", color=GRID, linewidth=0.7)
    for sp in ("top", "right"): ax.spines[sp].set_visible(False)
    for sp in ("left", "bottom"): ax.spines[sp].set_color(BASELINE)
    ax.tick_params(colors=MUTED, labelsize=9)
    ax.set_xlim(T_MIN, T_MAX)


def plot(ax, data, idx, color, label):
    xs = [t for t, b, wt in data if T_MIN <= t <= T_MAX]
    ys = [(b if idx == 1 else wt) for t, b, wt in data if T_MIN <= t <= T_MAX]
    ax.plot(xs, ys, color=color, linewidth=2, label=label, solid_capstyle="round")


# ── 패널 1: busy request threads (동기 점유 스레드) ──
plot(ax1, before, 1, C_BEFORE, "before · 동기 (요청 스레드가 AI 응답까지 직접 대기)")
plot(ax1, after, 1, C_AFTER, "after · 비동기 (@Async 위임 후 즉시 반납)")
ax1.axhline(REQ_POOL, color=MUTED, linewidth=1, linestyle=(0, (4, 3)))
ax1.text(T_MIN + 0.5, REQ_POOL + 0.4, f"요청 스레드풀 상한 = {int(REQ_POOL)}", ha="left", va="bottom", fontsize=8.5, color=MUTED)
ax1.set_ylim(0, REQ_POOL * 1.28)
ax1.set_ylabel("busy request threads\n(AI 응답 대기 중 스레드)", fontsize=9.5, color=INK2)
ax1.legend(loc="upper right", fontsize=8.5, frameon=False, labelcolor=INK2)
pmax_b = max((b for t, b, wt in before), default=0)
if pmax_b:
    ax1.annotate("풀 상한까지 스레드 누적·포화", xy=(1, pmax_b), xytext=(3, REQ_POOL * 1.14),
                 fontsize=9, color=C_BEFORE, arrowprops=dict(arrowstyle="-", color=C_BEFORE, lw=0.8))

# ── 패널 2: waiting request threads (요청 대기 큐) ──
plot(ax2, before, 2, C_BEFORE, "before")
plot(ax2, after, 2, C_AFTER, "after")
ax2.set_ylabel("waiting request threads\n(풀 자리 대기 = 큐)", fontsize=9.5, color=INK2)
ax2.set_xlabel("부하 시작 후 경과 시간 (초)", fontsize=9.5, color=INK2)
wmax_b = max((wt for t, b, wt in before), default=0)
if wmax_b:
    ax2.annotate(f"최대 {int(wmax_b)}건 대기(큐잉)", xy=(1, wmax_b), xytext=(3, wmax_b * 0.7),
                 fontsize=9, color=C_BEFORE, arrowprops=dict(arrowstyle="-", color=C_BEFORE, lw=0.8))
ax2.annotate("after: 대기 없음 (0)", xy=(T_MAX * 0.5, 0), xytext=(T_MAX * 0.42, max(wmax_b, 1) * 0.35),
             fontsize=9, color=C_AFTER, arrowprops=dict(arrowstyle="-", color=C_AFTER, lw=0.8))

fig.suptitle("트러블 1 — 동기 대기·스레드 누적: 비동기 전환 before/after", fontsize=12.5,
             color=INK, x=0.02, ha="left", y=0.985)
sub = "로컬 재현 · 요청 60건 동시 제출 · AI 응답 5s 모사 · 요청풀 20 / 워커풀 8"
if sb and sa:
    sub += f"  |  제출 응답시간 avg: before {sb['avg']}ms → after {sa['avg']}ms · max: {sb['max']}ms → {sa['max']}ms"
fig.text(0.02, 0.925, sub, fontsize=8.3, color=INK2)

fig.tight_layout(rect=(0, 0, 1, 0.92))
fig.savefig(OUT, facecolor=SURFACE, bbox_inches="tight")
print(f"saved -> {OUT}")
