# -*- coding: utf-8 -*-
"""트러블 1 — 응답시간(제출→수락) before/after 차트.

부하 생성기(load.py)가 실측한 요청별 응답시간(load_sync.csv / load_async.csv)으로
  ① 합본 비교  async-latency-before-after.png
  ② 단독 before  async-latency-before.png
  ③ 단독 after   async-latency-after.png
를 생성한다. 단독 2장은 y축 스케일을 동일 고정해 나란히 정직하게 대비되게 한다.

스토리: 동기 처리에선 요청 스레드가 AI 응답까지 대기 → 풀 포화 후 뒤 요청이 큐에서 밀려
'계단식'으로 응답시간이 폭증. 비동기(@Async) 전환 후엔 즉시 수락(202)되어 평탄·저지연.
"""
import csv, os, sys
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "data")

SURFACE="#fcfcfb"; INK="#0b0b0b"; INK2="#52514e"; MUTED="#898781"
GRID="#e1e0d9"; BASELINE="#c3c2b7"; C_BEFORE="#e34948"; C_AFTER="#2a78d6"
plt.rcParams["font.family"] = "Malgun Gothic"
plt.rcParams["axes.unicode_minus"] = False

SUB = "로컬 재현 · 요청 60건 동시 제출 · AI 응답 5s 모사 · 요청풀 20 / 워커풀 8 · 응답시간 = 제출→수락(초)"


def load_lat(path):
    lat = []
    with open(path, encoding="utf-8") as f:
        for r in csv.DictReader(f):
            if r["status"] in ("200", "202"):
                lat.append(float(r["elapsed_ms"]) / 1000.0)  # 초
    return sorted(lat)


def stats(lat):
    n = len(lat)
    return {
        "avg": sum(lat) / n,
        "p95": lat[max(0, int(n * 0.95) - 1)],
        "max": lat[-1],
    }


def style(ax):
    ax.set_facecolor(SURFACE)
    ax.grid(True, axis="y", color=GRID, linewidth=0.7)
    for sp in ("top", "right"): ax.spines[sp].set_visible(False)
    for sp in ("left", "bottom"): ax.spines[sp].set_color(BASELINE)
    ax.tick_params(colors=MUTED, labelsize=9)


b = load_lat(os.path.join(DATA, "load_sync.csv"))
a = load_lat(os.path.join(DATA, "load_async.csv"))
sb, sa = stats(b), stats(a)
YMAX = (max(sb["max"], sa["max"]) // 2 + 1) * 2  # 짝수 상한
NB, NA = len(b), len(a)


def curve(ax, data, color, label):
    ax.plot(range(1, len(data) + 1), data, color=color, linewidth=2.2,
            solid_capstyle="round", label=label)


def bars(ax, keys=("avg", "p95", "max")):
    import numpy as np
    x = list(range(len(keys)))
    wb = 0.36
    ax.bar([i - wb/2 for i in x], [sb[k] for k in keys], wb, color=C_BEFORE, label="before · 동기")
    ax.bar([i + wb/2 for i in x], [sa[k] for k in keys], wb, color=C_AFTER, label="after · 비동기")
    for i, k in enumerate(keys):
        ax.text(i - wb/2, sb[k] + YMAX*0.015, f"{sb[k]:.1f}s", ha="center", va="bottom", fontsize=8.5, color=C_BEFORE)
        ax.text(i + wb/2, sa[k] + YMAX*0.015, f"{sa[k]:.1f}s", ha="center", va="bottom", fontsize=8.5, color=C_AFTER)
    ax.set_xticks(x); ax.set_xticklabels(["평균", "p95", "최대"], fontsize=9.5, color=INK2)
    ax.set_ylim(0, YMAX * 1.12)


# ── ① 합본 ──
def combined(out):
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(10, 6.8), dpi=150, gridspec_kw={"height_ratios": [1.45, 1]})
    fig.patch.set_facecolor(SURFACE)
    for ax in (ax1, ax2): style(ax)
    curve(ax1, b, C_BEFORE, f"before · 동기 (요청 스레드가 AI 응답까지 대기)")
    curve(ax1, a, C_AFTER, f"after · 비동기 (@Async 위임 후 즉시 202)")
    ax1.set_ylim(0, YMAX)
    ax1.set_xlim(1, max(NB, NA))
    ax1.set_ylabel("응답시간 (초)", fontsize=9.5, color=INK2)
    ax1.set_xlabel("요청 (응답시간 오름차순 정렬)", fontsize=9.5, color=INK2)
    ax1.legend(loc="upper left", fontsize=9, frameon=False, labelcolor=INK2)
    ax1.annotate("풀 포화 → 뒤 요청이 큐에서 밀려\n계단식으로 지연 폭증",
                 xy=(NB*0.8, b[int(NB*0.8)]), xytext=(NB*0.30, YMAX*0.72),
                 fontsize=9, color=C_BEFORE, arrowprops=dict(arrowstyle="-", color=C_BEFORE, lw=0.8))
    ax1.annotate("비동기: 전 구간 평탄·저지연", xy=(NA*0.5, a[int(NA*0.5)]), xytext=(NA*0.42, YMAX*0.22),
                 fontsize=9, color=C_AFTER, arrowprops=dict(arrowstyle="-", color=C_AFTER, lw=0.8))
    bars(ax2)
    ax2.set_ylabel("응답시간 (초)", fontsize=9.5, color=INK2)
    ax2.legend(loc="upper right", fontsize=9, frameon=False, labelcolor=INK2)
    fig.suptitle("트러블 1 — 응답시간(제출→수락) before/after : 동기 지연 → 비동기 개선",
                 fontsize=12.5, color=INK, x=0.02, ha="left", y=0.985)
    fig.text(0.02, 0.93, SUB, fontsize=8.3, color=INK2)
    fig.tight_layout(rect=(0, 0, 1, 0.925))
    fig.savefig(out, facecolor=SURFACE, bbox_inches="tight"); plt.close(fig)
    print(f"saved -> {out}")


# ── ②③ 단독 (y축 동일 고정) ──
def single(data, st, n, color, title, tag, out):
    fig, ax = plt.subplots(figsize=(9, 4.6), dpi=150)
    fig.patch.set_facecolor(SURFACE); style(ax)
    curve(ax, data, color, tag)
    ax.set_ylim(0, YMAX); ax.set_xlim(1, n)
    ax.set_ylabel("응답시간 (초)", fontsize=9.5, color=INK2)
    ax.set_xlabel("요청 (응답시간 오름차순 정렬)", fontsize=9.5, color=INK2)
    ax.text(0.985, 0.94, f"평균 {st['avg']:.1f}s   p95 {st['p95']:.1f}s   최대 {st['max']:.1f}s",
            transform=ax.transAxes, ha="right", va="top", fontsize=10, color=color, fontweight="bold")
    fig.suptitle(title, fontsize=12.5, color=INK, x=0.02, ha="left", y=0.99)
    fig.text(0.02, 0.925, SUB, fontsize=8.3, color=INK2)
    fig.tight_layout(rect=(0, 0, 1, 0.9))
    fig.savefig(out, facecolor=SURFACE, bbox_inches="tight"); plt.close(fig)
    print(f"saved -> {out}")


if __name__ == "__main__":
    combined(os.path.join(HERE, "async-latency-before-after.png"))
    single(b, sb, NB, C_BEFORE, "트러블 1 — 응답시간 : before (동기, 요청 스레드가 AI 응답까지 대기)",
           "before · 동기", os.path.join(HERE, "async-latency-before.png"))
    single(a, sa, NA, C_AFTER, "트러블 1 — 응답시간 : after (비동기, @Async 위임 후 즉시 202)",
           "after · 비동기", os.path.join(HERE, "async-latency-after.png"))
