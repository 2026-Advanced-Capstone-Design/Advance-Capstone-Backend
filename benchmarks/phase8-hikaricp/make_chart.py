# -*- coding: utf-8 -*-
"""Phase 8 before/after HikariCP 비교 차트 생성."""
import csv
import sys
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

OUT = sys.argv[1] if len(sys.argv) > 1 else "phase8-hikaricp-before-after.png"

# ── 팔레트 (dataviz 검증 기본 팔레트, 라이트 모드) ──
SURFACE = "#fcfcfb"
INK = "#0b0b0b"
INK2 = "#52514e"
MUTED = "#898781"
GRID = "#e1e0d9"
BASELINE = "#c3c2b7"
C_BEFORE = "#e34948"  # categorical slot 6 (red)
C_AFTER = "#2a78d6"   # categorical slot 1 (blue)

plt.rcParams["font.family"] = "Malgun Gothic"
plt.rcParams["axes.unicode_minus"] = False


def load(path):
    rows = []
    with open(path, encoding="utf-8") as f:
        for r in csv.DictReader(f):
            if r["active"] == "":
                continue
            rows.append((float(r["elapsed_s"]), float(r["active"]), float(r["pending"])))
    # 부하 시작(첫 active>0) 기준으로 시간축 정렬
    t0 = next((t for t, a, p in rows if a > 0), 0)
    return [(t - t0, a, p) for t, a, p in rows]


before = load("hikari_before.csv")
after = load("hikari_after.csv")

T_MIN, T_MAX = -10, 165

fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(10, 6.4), sharex=True, dpi=150)
fig.patch.set_facecolor(SURFACE)

for ax in (ax1, ax2):
    ax.set_facecolor(SURFACE)
    ax.grid(True, axis="y", color=GRID, linewidth=0.7)
    for spine in ("top", "right"):
        ax.spines[spine].set_visible(False)
    for spine in ("left", "bottom"):
        ax.spines[spine].set_color(BASELINE)
    ax.tick_params(colors=MUTED, labelsize=9)
    ax.set_xlim(T_MIN, T_MAX)


def plot(ax, data, col, color, label):
    xs = [t for t, a, p in data if T_MIN <= t <= T_MAX]
    ys = [(a if col == "a" else p) for t, a, p in data if T_MIN <= t <= T_MAX]
    ax.plot(xs, ys, color=color, linewidth=2, label=label, solid_capstyle="round")
    return xs, ys


# ── 패널 1: active ──
plot(ax1, before, "a", C_BEFORE, "before (트랜잭션이 AI HTTP 호출을 감쌈)")
plot(ax1, after, "a", C_AFTER, "after (트랜잭션 경계 축소, 현재 코드)")
ax1.axhline(10, color=MUTED, linewidth=1, linestyle=(0, (4, 3)))
ax1.text(T_MIN + 2, 10.4, "풀 상한 max=10", ha="left", va="bottom", fontsize=8.5, color=MUTED)
ax1.set_ylim(0, 12.5)
ax1.set_ylabel("active (사용 중 커넥션)", fontsize=9.5, color=INK2)
ax1.legend(loc="upper right", fontsize=9, frameon=False, labelcolor=INK2)

# before 구간 직접 라벨
ax1.annotate("~135초간 풀 포화 지속", xy=(60, 10), xytext=(66, 11.6),
             fontsize=9, color=C_BEFORE,
             arrowprops=dict(arrowstyle="-", color=C_BEFORE, lw=0.8))

# ── 패널 2: pending ──
plot(ax2, before, "p", C_BEFORE, "before")
plot(ax2, after, "p", C_AFTER, "after")
ax2.set_ylabel("pending (커넥션 대기 스레드)", fontsize=9.5, color=INK2)
ax2.set_xlabel("부하 시작 후 경과 시간 (초)", fontsize=9.5, color=INK2)

pmax_b = max(p for t, a, p in before if T_MIN <= t <= T_MAX)
ax2.annotate(f"최대 {int(pmax_b)}개 스레드 대기", xy=(0, pmax_b), xytext=(14, pmax_b * 0.86),
             fontsize=9, color=C_BEFORE,
             arrowprops=dict(arrowstyle="-", color=C_BEFORE, lw=0.8))
ax2.annotate("after: 전 구간 pending = 0", xy=(110, 0), xytext=(105, pmax_b * 0.30),
             fontsize=9, color=C_AFTER,
             arrowprops=dict(arrowstyle="-", color=C_AFTER, lw=0.8))

fig.suptitle("Phase 8 — HikariCP 커넥션 풀: 트랜잭션 경계 정리 before/after", fontsize=12.5,
             color=INK, x=0.02, ha="left", y=0.985)
fig.text(0.02, 0.925,
         "로컬 재현 · MySQL 8.0 · 풀 max=10 · AI 스텁 응답 10s 지연 · 부하: 분석 제출 108건 동시 버스트 + 상태 폴링 100스레드×90s · 2026-07-11",
         fontsize=8.5, color=INK2)

fig.tight_layout(rect=(0, 0, 1, 0.92))
fig.savefig(OUT, facecolor=SURFACE, bbox_inches="tight")
print(f"saved -> {OUT}")
