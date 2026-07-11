# -*- coding: utf-8 -*-
"""Phase 8 HikariCP 단독 차트 2장 생성 (before 1장, after 1장).

비교용 합본(make_chart.py)과 달리 한 장에 한 버전만 담되,
두 장의 y축 스케일을 동일하게 고정해 나란히 놓았을 때 정직하게 대비되게 한다.
"""
import csv
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

# ── 팔레트 (dataviz 검증 기본 팔레트, 라이트 모드) ──
SURFACE = "#fcfcfb"
INK = "#0b0b0b"
INK2 = "#52514e"
MUTED = "#898781"
GRID = "#e1e0d9"
BASELINE = "#c3c2b7"
C_BEFORE = "#e34948"
C_AFTER = "#2a78d6"

plt.rcParams["font.family"] = "Malgun Gothic"
plt.rcParams["axes.unicode_minus"] = False

T_MIN, T_MAX = -10, 165
Y_ACTIVE = (0, 12.5)
Y_PENDING = (0, 105)

SUBTITLE = ("로컬 재현 · MySQL 8.0 · 풀 max=10 · AI 스텁 응답 10s 지연 · "
            "부하: 분석 제출 108건 동시 버스트 + 상태 폴링 100스레드×90s · 2026-07-11")


def load(path):
    rows = []
    with open(path, encoding="utf-8") as f:
        for r in csv.DictReader(f):
            if r["active"] == "":
                continue
            rows.append((float(r["elapsed_s"]), float(r["active"]), float(r["pending"])))
    t0 = next((t for t, a, p in rows if a > 0), 0)
    return [(t - t0, a, p) for t, a, p in rows]


def render(data, color, title, out, annotate):
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

    xs = [t for t, a, p in data if T_MIN <= t <= T_MAX]
    ya = [a for t, a, p in data if T_MIN <= t <= T_MAX]
    yp = [p for t, a, p in data if T_MIN <= t <= T_MAX]

    ax1.plot(xs, ya, color=color, linewidth=2, solid_capstyle="round")
    ax1.axhline(10, color=MUTED, linewidth=1, linestyle=(0, (4, 3)))
    ax1.text(T_MIN + 2, 10.4, "풀 상한 max=10", ha="left", va="bottom", fontsize=8.5, color=MUTED)
    ax1.set_ylim(*Y_ACTIVE)
    ax1.set_ylabel("active (사용 중 커넥션)", fontsize=9.5, color=INK2)

    ax2.plot(xs, yp, color=color, linewidth=2, solid_capstyle="round")
    ax2.set_ylim(*Y_PENDING)
    ax2.set_ylabel("pending (커넥션 대기 스레드)", fontsize=9.5, color=INK2)
    ax2.set_xlabel("부하 시작 후 경과 시간 (초)", fontsize=9.5, color=INK2)

    annotate(ax1, ax2, xs, ya, yp)

    fig.suptitle(title, fontsize=12.5, color=INK, x=0.02, ha="left", y=0.985)
    fig.text(0.02, 0.925, SUBTITLE, fontsize=8.5, color=INK2)
    fig.tight_layout(rect=(0, 0, 1, 0.92))
    fig.savefig(out, facecolor=SURFACE, bbox_inches="tight")
    plt.close(fig)
    print(f"saved -> {out}")


def annotate_before(ax1, ax2, xs, ya, yp):
    ax1.annotate("~135초간 풀 포화 지속\n(HTTP 응답 대기 내내 커넥션 점유)",
                 xy=(60, 10), xytext=(62, 11.0),
                 fontsize=9, color=C_BEFORE, va="bottom",
                 arrowprops=dict(arrowstyle="-", color=C_BEFORE, lw=0.8))
    pmax = max(yp)
    ax2.annotate(f"최대 {int(pmax)}개 스레드가 커넥션 대기",
                 xy=(0, pmax), xytext=(14, pmax * 0.86),
                 fontsize=9, color=C_BEFORE,
                 arrowprops=dict(arrowstyle="-", color=C_BEFORE, lw=0.8))


def annotate_after(ax1, ax2, xs, ya, yp):
    ax1.annotate("버스트 순간만 점유 후 즉시 반납\n(HTTP 대기 중 커넥션 없음)",
                 xy=(0, max(ya)), xytext=(18, 10.8),
                 fontsize=9, color=C_AFTER, va="bottom",
                 arrowprops=dict(arrowstyle="-", color=C_AFTER, lw=0.8))
    ax2.annotate("전 구간 pending = 0 (동일 부하)",
                 xy=(80, 0), xytext=(70, 30),
                 fontsize=9, color=C_AFTER,
                 arrowprops=dict(arrowstyle="-", color=C_AFTER, lw=0.8))


if __name__ == "__main__":
    render(load("data/hikari_before.csv"), C_BEFORE,
           "Phase 8 — HikariCP 커넥션 풀 : before (트랜잭션이 AI HTTP 호출을 감쌈, 9314f69 이전)",
           "phase8-hikaricp-before.png", annotate_before)
    render(load("data/hikari_after.csv"), C_AFTER,
           "Phase 8 — HikariCP 커넥션 풀 : after (트랜잭션 경계 축소, 현재 코드)",
           "phase8-hikaricp-after.png", annotate_after)
