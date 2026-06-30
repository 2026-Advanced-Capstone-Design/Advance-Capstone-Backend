# 뉴스 편향 분석 · 팩트체크 시스템 (NewsLens)

> 2026 심화 캡스톤 디자인 프로젝트
> 뉴스 기사의 편향성·사실성을 분석하는 프로젝트 
> 사용자가 한쪽으로 치우치지 않고 뉴스를 읽을 수 있도록 돕는 서비스.
---

## 1. 핵심 기능 (구현 완료)

###  뉴스 편향 · 사실성 분석
- **입력 3종**: 텍스트 직접 입력 / 뉴스 URL(크롤링) / 이미지(OCR)
- **편향 분석**: GPT 기반 Chain-of-Thought 2단계 추론
  - `vocab` — 감정적·편향적 어휘 사용 여부
  - `fact_basis` — 주장이 검증 가능한 데이터·출처에 근거하는지
- **문장 하이라이팅**: 문제 문장을 `fact / emotion / section_bias` 3유형으로 분류·시각화
- **팩트체크**: Google Fact Check API 근거 제시
- **요약·배경지식·키워드** 자동 생성

###  여론 분석 (유튜브 댓글)
- YouTube Data API v3로 관련 영상 댓글 수집
- **감정 분석**: `klue/roberta-base` 파인튜닝 3클래스 모델(부정/긍정/중립)
- **봇·스팸 탐지**: AI 탐지 모델 + 규칙 기반(TF-IDF 중복, 선동 키워드 등) 하이브리드
- **여론 요약**: 긍정/부정/중립 감정별 요약 분리 제공

---

## 2. 실행 화면 및 결과


## 3. 시스템 아키텍처

```
<img width="2866" height="2048" alt="image" src="https://github.com/user-attachments/assets/c741609e-a514-42a5-a3d6-111d3b97ed9d" />

```

### 기술 스택
| 영역 | 스택 |
|------|------|
| 백엔드 | Java 17, Spring Boot 3.5.x, Spring Data JPA, MySQL, Caffeine |
| AI 엔진 | Python, Flask, OpenAI GPT-4o / 4o-mini, BeautifulSoup, KoNLPy |
| 프론트 | React (CRA) |
| 확장 | Chrome Extension (Manifest) |
| 인프라 | Docker / Docker Compose, AWS EC2, GitHub Actions, JMeter |
| 외부 API | YouTube Data API v3, Google Fact Check API, Naver 뉴스 API |

---

## 4. AI 분석 파이프라인 상세

```
<img width="4044" height="2275" alt="image" src="https://github.com/user-attachments/assets/013b7078-0468-40ae-8a7c-11a6e59e93da" />

```

- **Model Tiering**: 배경지식 생성은 저비용 `gpt-4o-mini`, 최종 편향 검증은 `gpt-4o`로 분기해 비용·품질 균형
- **비동기 폴링**: GPT 호출(수 초~십수 초) 동안 `GET /status`로 진행 상태 조회

---

## 5. 프로젝트 구성 (멀티 레포)

| 레포 | 설명 | 주요 패키지/모듈 |
|------|------|------------------|
| **Backend** | Spring Boot API 서버 | `com.factcheck` (article 분석) + `com.factcheck.youtube` (여론 분석) |
| **AI 엔진** | Flask 분석 서버 | `routes/`(analyze·status), `services/`(analyzer·summarizer·labeler·factcheck·cache·preprocessor) |
| **감정분석 모델** | RoBERTa 파인튜닝/배포 | `src/train/` (학습·업로드), `models/sentiment_3class/` |
| **Frontend** | React 웹 | 분석 결과 시각화 |
| **Chrome Extension** | 크롬 확장 | 페이지 내 분석 요청 |

### 백엔드 주요 서비스
- `ArticleService` · `AnalysisCallbackService` — 분석 요청/콜백 처리
- `CrawlerService`(Jsoup) · `OcrService`(Tess4J) · `PreprocessService`(Komoran) — 입력 처리
- `AiWorkerClient` — Spring ↔ Flask 비동기 연동
- `NaverNewsService` — 관련 기사 수집
- `youtube/*` — 댓글 수집 + 여론 분석 + 비동기 처리

---

## 6. 인프라 & 운영

- **컨테이너화**: Backend / AI 엔진 각각 Dockerfile + `docker-compose.yaml`(로컬) / `docker-compose.prod.yaml`(운영)
- **배포**: AWS EC2 + GitHub Actions 자동 배포 (`.github/workflows/deploy.yml`)
- **부하 테스트**: JMeter 시나리오(`jmeter/`)로 동시 요청 성능 측정

---

## 7. 성능 개선 & 리팩토링

### 완료된 개선 (Before / After)
| 항목 | Before | After |
|------|--------|-------|
| AI 분석 응답 시간 | 약 29초 | 약 14초 |
| DB 조회 | N+1 발생 | fetch join으로 제거 |
| SQL 로깅 | 미설정 | 쿼리 로깅 구성 |

### 진행 중 — 리팩토링 마스터 플랜
> 동시성·메모리 누수 해결을 중심축으로, **측정(모니터링) → 개선 → 재측정** 원칙. 상세: [`REFACTORING_PLAN.md`](./REFACTORING_PLAN.md)

1. **모니터링·로그 인프라** — Prometheus + Grafana + Loki
2. **AI 엔진 Flask → FastAPI 전환** — I/O 바운드 비동기화, task 저장소 인메모리 dict → Redis(TTL)
3. **메모리 누수 차단** — 캐시 TTL/eviction, 업로드 파일 정리, OCR 네이티브 메모리 해제
4. **동시성 정합성 + 장애 대응** — 캐시 스탬피드 방지, Alerting, 장애 주입 회고
5. **클라우드 운영 강화** — VPC/ALB/RDS, 무중단 배포
6. **개인화 + 인증·인가 + API 버전관리** — 하이브리드 식별, Spring Security + JWT

---

## 8. 로컬 실행

### 사전 요구사항
- Java 17+, Docker & Docker Compose, Node.js 18+, Python 3.10+

### 백엔드 + AI 엔진
```bash
# 백엔드 레포에서
docker-compose up -d        # MySQL + Backend + AI 엔진
# Swagger: http://localhost:8080/swagger-ui.html
```

### 감정분석 모델 (이 레포)
```powershell
& venv\Scripts\Activate.ps1
python src/test_real_comments.py          # 유튜브 댓글 분석 파이프라인
python src/train/train_sentiment_3class.py # 모델 재학습 (GPU 필요)
```

### 환경 변수 (`.env`)
- `OPENAI_API_KEY`, `YOUTUBE_API_KEY`, `GOOGLE_FACTCHECK_API_KEY`, `HUGGINGFACE_TOKEN`

---


### PR 규칙
- `develop` 브랜치로 PR, 최소 1인 리뷰 승인 후 머지, 로컬 빌드(`./gradlew build`) 성공 확인
