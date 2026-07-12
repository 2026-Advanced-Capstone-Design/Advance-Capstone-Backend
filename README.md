<div align="center">

#  NewsLens — AI 기반 뉴스 편향 분석 · 팩트체크 시스템

**한 사건을 여러 언론사 관점에서 비교해, 편향에 치우치지 않고 균형 있게 판단하도록 돕는 서비스**

뉴스 기사의 **정치적 편향·사실성을 AI로 분석**하고, 유튜브 댓글로 **여론을 분석**하여 시각화합니다.

<br/>

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![Python](https://img.shields.io/badge/Python-Flask-3776AB?logo=python&logoColor=white)
![GPT-4o](https://img.shields.io/badge/OpenAI-GPT--4o-412991?logo=openai&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker%20Compose-2496ED?logo=docker&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-EC2%20%7C%20ALB-FF9900?logo=amazonaws&logoColor=white)
![Grafana](https://img.shields.io/badge/Prometheus%20%2B%20Grafana-E6522C?logo=grafana&logoColor=white)

<br/>

<!-- 📷 서비스 데모 GIF 자리 → docs/images/demo.gif -->
<img src="docs/images/demo.gif" width="760" alt="NewsLens 서비스 데모"/>
<br/>
<sub>▲ 서비스 데모 — <code>docs/images/demo.gif</code> (이미지 추가 예정)</sub>

</div>

---

---

# Part 1. 서비스 개요

## 1.1 핵심 기능

### 📰 뉴스 편향 · 사실성 분석
- **입력 3종**: 텍스트 직접 입력 / 뉴스 URL(크롤링) / 이미지(OCR)
- **편향 분석**: GPT 기반 Chain-of-Thought 2단계 추론
  - `vocab` — 감정적·편향적 어휘 사용 여부
  - `fact_basis` — 주장이 검증 가능한 데이터·출처에 근거하는지
- **문장 하이라이팅**: 문제 문장을 `fact / emotion / section_bias` 3유형으로 분류·시각화
- **팩트체크**: Google Fact Check API 근거 제시
- **요약 · 배경지식 · 키워드** 자동 생성

<!-- 📷 분석 결과 화면 자리 → docs/images/analysis-result.png -->
<p align="center">
  <img src="docs/images/analysis-result.png" width="720" alt="편향·팩트체크 분석 결과 화면"/><br/>
  <sub>▲ 편향·팩트체크 결과 화면 — <code>docs/images/analysis-result.png</code> (이미지 추가 예정)</sub>
</p>

### 💬 여론 분석 (유튜브 댓글)
- YouTube Data API v3로 관련 영상 댓글 수집
- **감정 분석**: `klue/roberta-base` 파인튜닝 3클래스 모델(부정 / 긍정 / 중립)
- **봇 · 스팸 탐지**: AI 탐지 모델 + 규칙 기반(TF-IDF 중복, 선동 키워드 등) 하이브리드
- **여론 요약**: 긍정 / 부정 / 중립 감정별 요약 분리 제공

<!-- 📷 유튜브 여론 분석 화면 자리 → docs/images/youtube-opinion.png -->
<p align="center">
  <img src="docs/images/youtube-opinion.png" width="720" alt="유튜브 여론 분석 결과 화면"/><br/>
  <sub>▲ 유튜브 여론 분석 화면 — <code>docs/images/youtube-opinion.png</code> (이미지 추가 예정)</sub>
</p>

### 🧩 크롬 확장 프로그램
- 뉴스 페이지에서 바로 분석 요청 → 결과 오버레이

---

## 1.2 시스템 아키텍처

<!-- 📷 아키텍처 다이어그램 자리 → docs/images/architecture.png -->
<p align="center">
  <img src="docs/images/architecture.png" width="820" alt="시스템 아키텍처 다이어그램"/><br/>
  <sub>▲ 시스템 아키텍처 — <code>docs/images/architecture.png</code> (이미지 추가 예정)</sub>
</p>

<details>
<summary>텍스트 아키텍처 (이미지 대체용)</summary>

```
[크롬 확장 / React 프론트]
            │  REST
            ▼
   [Spring Boot 백엔드]  ──┐
   - 입력 분기(TEXT/URL/IMAGE)     │ 크롤링(Jsoup) · OCR(Tess4J) · 전처리(Komoran)
   - 비동기 분석 오케스트레이션      │ 외부 API(Naver 뉴스, Google Fact Check)
            │  비동기 호출/콜백       │
            ▼                       │
   [AI 엔진 (Flask)]  ◀─────────────┘
   - Generated Knowledge → CoT 편향분석 → 문장 하이라이팅
   - Topic 캐싱 / Model Tiering (gpt-4o-mini ↔ gpt-4o)
            │
            ▼
   [감정분석 모델 (RoBERTa, HuggingFace)]  ── 유튜브 여론 분석
```
</details>

### 기술 스택
| 영역 | 스택 |
|------|------|
| 백엔드 | Java 17, Spring Boot 3.5.x, Spring Data JPA, MySQL, Redis, Caffeine |
| AI 엔진 | Python, Flask, OpenAI GPT-4o / 4o-mini, BeautifulSoup, KoNLPy, ChromaDB |
| 감정분석 모델 | PyTorch, HuggingFace Transformers, `klue/roberta-base` 파인튜닝 |
| 프론트 | React (CRA) |
| 확장 | Chrome Extension (Manifest) |
| 인프라 | Docker / Docker Compose, AWS EC2 · ALB, GitHub Actions, JMeter |
| 모니터링 | Prometheus, Grafana, Micrometer |
| 외부 API | YouTube Data API v3, Google Fact Check API, Naver 뉴스 API |

---

## 1.3 데이터베이스 ERD

<!-- 📷 ERD 이미지 자리 → docs/images/erd.png -->
<p align="center">
  <img src="docs/images/erd.png" width="820" alt="데이터베이스 ERD"/><br/>
  <sub>▲ ERD — <code>docs/images/erd.png</code> (이미지 추가 예정)</sub>
</p>

---

## 1.4 AI 분석 파이프라인

```
입력 수신 (텍스트 / URL→크롤링 / 이미지→OCR)
  └─ 전처리 (BeautifulSoup + KoNLPy + 정규식)
      └─ [Generated Knowledge] 이슈 배경지식 생성 (gpt-4o-mini)
          └─ [Topic Caching] 동일 이슈면 DB 재사용 → GPT 호출 절감
              └─ [CoT 편향분석] vocab + fact_basis 2단계 추론 (gpt-4o)
                  └─ [문장 하이라이팅] fact / emotion / section_bias 분류 (gpt-4o-mini)
                      └─ [팩트체크] Google Fact Check API 근거 결합
                          └─ 점수 집계 → 결과 반환 (Spring 콜백)
```

- **Model Tiering** — 배경지식 생성은 저비용 `gpt-4o-mini`, 최종 편향 검증은 `gpt-4o`로 분기해 비용·품질 균형
- **비동기 폴링** — GPT 호출(수 초~십수 초) 동안 `GET /status`로 진행 상태 조회

---

## 1.5 프로젝트 구성 (멀티 레포)

| 레포 | 설명 | 주요 모듈 |
|------|------|-----------|
| **Backend** | Spring Boot API 서버 | `com.factcheck`(기사 분석) + `com.factcheck.youtube`(여론 분석) |
| **AI 엔진** | Flask 분석 서버 | `routes/`(analyze·status), `services/`(analyzer·summarizer·labeler·factcheck·cache) |
| **감정분석 모델** | RoBERTa 파인튜닝/배포 | `src/train/`, `models/sentiment_3class/` |
| **Frontend** | React 웹 | 분석 결과 시각화 |
| **Chrome Extension** | 크롬 확장 | 페이지 내 분석 요청 |

---

<br/>

# Part 2. 리팩토링 & 트러블슈팅

> **핵심 원칙: 추측이 아니라 "측정 → 개선 → 재측정".**
> 모든 성능 수치는 학습 결과가 아니라 **운영 환경 부하 테스트** 기반입니다.
> 측정 환경·기준·방법은 [`benchmark-method.md`](./Advance-Capstone-Backend/docs/benchmark-method.md), 시행착오 기록은 [`troubleshooting.md`](./Advance-Capstone-Backend/docs/troubleshooting.md) 참고.

### 모니터링 기반 측정
Prometheus + Grafana로 JVM 힙 · HikariCP 커넥션풀 · 스레드풀 큐 · AI 지연을 상시 관측하고, **모든 개선의 before/after 근거**로 사용했습니다.

<!-- 📷 Grafana 대시보드 자리 → docs/images/grafana-overview.png -->
<p align="center">
  <img src="docs/images/grafana-overview.png" width="760" alt="Grafana 모니터링 대시보드"/><br/>
  <sub>▲ Grafana 대시보드 — <code>docs/images/grafana-overview.png</code> (이미지 추가 예정)</sub>
</p>

---

## 트러블 1 — 동기 구조 → 비동기 · 분산 구조 개편

| 구분 | 내용 |
|------|------|
| **문제 · 측정** | 동기식 단일 EC2에서 HTTP 요청이 GPT 응답(3~10초)까지 **동기 대기** → 타임아웃 · 스레드 누적. 동일 URL도 매 요청 GPT 호출 → 비용 낭비 + 동시 쓰기 충돌 |
| **대안 → 채택** | ❌ 타임아웃만 상향(스레드 누적 근본 미해결) → ✅ **`@Async` 전용 스레드풀**로 HTTP 응답과 디커플링 + **URL 해시 캐싱** + **ArticleStatus 상태 전이**로 중복·경쟁 차단 + **ALB 다중 EC2**(Health Check 기반 장애 인스턴스 자동 제외) |
| **검증 · 결과** | GPT 3~10초 응답에도 타임아웃 없이 요청 수락 · 최대 8건 병렬 처리 · 중복 URL GPT 호출 0회 · 인스턴스 장애 시 무중단 운영 |
| **사용자 가치** | 요청이 몰려도 화면이 멈추지 않고 즉시 접수되며, 분석된 기사는 바로 열린다 |

<!-- 📷 AS-IS/TO-BE 구조도 자리 → docs/images/async-asis-tobe.png -->
<p align="center">
  <img src="docs/images/async-asis-tobe.png" width="760" alt="동기 → 비동기·분산 구조 AS-IS / TO-BE"/><br/>
  <sub>▲ AS-IS → TO-BE 구조도 — <code>docs/images/async-asis-tobe.png</code> (이미지 추가 예정)</sub>
</p>

---

## 트러블 2 — 트랜잭션 경계 정리 + `@Transactional` 프록시 자기호출 함정

| 구분 | 내용 |
|------|------|
| **문제 · 측정** | **경계 결함** — `@Async`+`@Transactional` 메서드가 AI HTTP 호출(3~10초) 내내 **DB 커넥션 점유** → 부하 시 HikariCP 고갈. **프록시 함정** — 같은 클래스의 `@Transactional` 메서드를 **자기호출**해 프록시가 우회되어 트랜잭션 미적용(잠복) |
| **대안 → 채택** | ❌ pool size 증설(점유 시간은 그대로 — 과거 튜닝 삽질의 근본 원인) → ✅ **트랜잭션 경계를 상태 업데이트(짧은 tx)로 축소**하고 **HTTP 호출은 트랜잭션 밖**에서 수행. 상태 쓰기를 `REQUIRES_NEW` 별도 빈으로 분리해 자기호출·경계를 동시 해소 |
| **검증 · 결과 (JMeter 500스레드 · 5,000요청)** | 에러율 **8.58% → 0.00%** · HikariCP pending **98 → 0** · p95 **23ms** |
| **사용자 가치** | 트래픽이 몰려도 커넥션 고갈로 인한 전면 지연·실패 없이 정상 처리된다 |

<!-- 실제 벤치마크 차트 (파일 존재 → 바로 표시됨) -->
<p align="center">
  <img src="Advance-Capstone-Backend/benchmarks/phase8-hikaricp/phase8-hikaricp-before-after.png" width="820" alt="HikariCP pending before / after"/><br/>
  <sub>▲ 트랜잭션 경계 정리 전/후 — HikariCP pending & 에러율 (실측 차트)</sub>
</p>

---

## 트러블 3 — 비동기 콜백 파이프라인 정합성 · 신뢰성

| 구분 | 내용 |
|------|------|
| **문제 · 측정** | AI 엔진이 결과를 Spring으로 **fire-and-forget 푸시** → ① 중복 콜백 시 중복 INSERT → `NonUniqueResultException`(500) ② Spring 순간 다운 시 분석 결과 영구 유실 ③ 상태 쓰기 race로 `ANALYZING`이 `DONE`을 덮어써 "분석 중" stuck |
| **대안 → 채택** | ❌ Transactional Outbox / SQLite 영속(과설계로 보류 — 스위퍼가 최후 안전망) → ✅ **`ARTICLE_ID` UNIQUE + 멱등 가드** · **조건부 UPDATE(CAS)** 로 전이 순서 무관 정합 · **재조정 스위퍼** + **지수 백오프 재시도**(1s→2s→4s, 4xx 즉시 포기) |
| **검증 · 결과 (장애 주입)** | 결과 유실 0건 · 중복 콜백 500 0건 · "분석 중" stuck 제거. Spring 다운 중 분석 완료 → 복구 후 재시도 전달 성공 관찰 |
| **사용자 가치** | "분석 중"에서 멈추는 화면이 사라지고, 서버가 잠시 흔들려도 결과가 유실되지 않는다 |

<!-- 📷 콜백 파이프라인 흐름도 자리 → docs/images/callback-flow.png -->
<p align="center">
  <img src="docs/images/callback-flow.png" width="760" alt="멱등·CAS·스위퍼 콜백 파이프라인 흐름도"/><br/>
  <sub>▲ 콜백 파이프라인 흐름도 — <code>docs/images/callback-flow.png</code> (이미지 추가 예정)</sub>
</p>

---

## 트러블 4 — 메모리 누수 해결 (무한 증가 소스 차단)

| 구분 | 내용 |
|------|------|
| **문제 · 측정** | `AnalysisCache` DB · `uploads/images`가 **TTL·정리 없이 무한 증가**, 컨테이너 메모리 미제한. t2.micro **available ~286Mi** 압박 (docker stats · Grafana 힙) |
| **대안 → 채택** | ❌ 인스턴스 스케일업(비용↑, 누수 근본 미해결) → ✅ **스케줄러 정리**(캐시·업로드 만료 벌크 DELETE, 매일 04:30/04:40) · OCR `@PostConstruct` 1회 초기화 · **컨테이너 mem_limit** + `MaxRAMPercentage=60` · `open-in-view=false` · HikariCP pool 30→15 |
| **검증 · 결과** | OSIV 경고 소멸 · 캐시/업로드 주기 정리 동작 · 실분석 e2e 통과 *(힙덤프 정밀 분석은 진행 중)* |
| **사용자 가치** | 장시간 운영해도 메모리 고갈로 인한 서비스 중단 없이 안정적으로 동작한다 |

<!-- 📷 메모리 개선 전/후 자리 → docs/images/memory-before-after.png -->
<p align="center">
  <img src="docs/images/memory-before-after.png" width="760" alt="메모리 제한 적용 전/후"/><br/>
  <sub>▲ 메모리 개선 전/후 — <code>docs/images/memory-before-after.png</code> (이미지 추가 예정)</sub>
</p>

---

## 리팩토링 요약 (Before / After)

| 항목 | Before | After | 측정 |
|------|--------|-------|------|
| AI 분석 응답 시간 | 약 29초 | 약 14초 | 파이프라인 병렬화 · Model Tiering |
| 부하 시 에러율 | 8.58% | **0.00%** | JMeter 500스레드 · 5,000요청 |
| HikariCP pending | 98 | **0** | 트랜잭션 경계 축소 |
| 응답 p95 | — | 23ms | EC2 실측 |
| 콜백 결과 유실 | 간헐 발생 | **0건** | 멱등 + 재시도 + 장애 주입 검증 |
| DB 조회 | N+1 발생 | fetch join 제거 | 쿼리 로그 · EXPLAIN |

> ⚠️ 위 수치의 측정 환경(인스턴스 스펙 · MOCK_MODE · 동시 사용자 수)과 검증 기준은 [`benchmark-method.md`](./Advance-Capstone-Backend/docs/benchmark-method.md)에 명시되어 있습니다.

---

<br/>

# Part 3. 실행 & 컨벤션

## 3.1 로컬 실행

**사전 요구사항** — Java 17+, Docker & Docker Compose, Node.js 18+, Python 3.10+

```bash
# 백엔드 + AI 엔진 (백엔드 레포에서)
docker-compose up -d          # MySQL + Redis + Backend + AI 엔진
# Swagger: http://localhost:8080/swagger-ui.html
```

```powershell
# 감정분석 모델 (이 레포)
& venv\Scripts\Activate.ps1
python src/test_real_comments.py           # 유튜브 댓글 분석 파이프라인
python src/train/train_sentiment_3class.py # 모델 재학습 (GPU 필요)
```

**환경 변수 (`.env`)** — `OPENAI_API_KEY` · `YOUTUBE_API_KEY` · `GOOGLE_FACTCHECK_API_KEY` · `HUGGINGFACE_TOKEN`

## 3.2 컨벤션

```
main ← 배포용 (PR 머지만)
└── develop ← 개발 통합
    ├── feature/기능명   ├── fix/버그명   └── refactor/대상명
```

- **커밋** — `feat` · `fix` · `refactor` · `docs` · `test` · `chore` + 한 줄 설명
- **PR** — `develop` 브랜치로 PR, 최소 1인 리뷰 승인 후 머지, 로컬 빌드(`./gradlew build`) 성공 확인
