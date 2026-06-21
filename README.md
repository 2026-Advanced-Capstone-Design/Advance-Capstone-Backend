# 뉴스 렌즈 (News Lens) 🔍

  > 뉴스 기사·유튜브 댓글의 **편향성 분석 + 팩트체크 + 여론(감정) 분석**을 제공하는 AI 기반 미디어 리터러시 서비스

  <!-- 배지 예시 (선택) -->
  ![Python](https://img.shields.io/badge/Python-3.10-blue)
  ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
  ![License](https://img.shields.io/badge/license-MIT-lightgrey)

  ---

  ## 📌 프로젝트 소개

  - **무엇을**: (한 줄 요약 — 예: 뉴스 기사를 입력하면 편향도/사실관계/여론을 자동 분석)
  - **왜**: (문제의식 — 예: 가짜뉴스·편향 보도로 인한 정보 왜곡 문제)
  - **누가**: (대상 사용자 — 예: 뉴스를 비판적으로 소비하려는 일반 독자)
  - **기간 / 소속**: 2026.0X ~ 2026.0X · OO대학교 심화캡스톤

  ---

  ## ✨ 주요 기능

  | 기능 | 설명 |
  |------|------|
  | 감정 분석 (3클래스) | 댓글을 부정/긍정/중립으로 분류 (`klue/roberta-base` 파인튜닝) |
  | 봇·스팸 탐지 | AI 모델(50%) + 규칙 기반(50%) 하이브리드 판정 |
  | 여론 요약 | 긍정/부정/중립별 감정 요약 문장 생성 |
  | 뉴스 편향 분석 | (내용 채우기) |
  | 팩트체크 | Google FactCheck API 연동 사실관계 검증 |
  | 크롬 익스텐션 | 브라우저에서 바로 기사 분석 |

  ---

  ## 🏗️ 시스템 아키텍처

  ```
  [Chrome Extension] ──┐
  [Web Frontend]    ──┼──▶ [Spring Boot Backend] ──▶ [AI Engine (FastAPI/Python)]
                       │             │                        │
                       │             ▼                        ▼
                       │          [RDS / DB]          [HuggingFace 모델, LLM]
  ```

  | 레이어 | 기술 스택 |
  |--------|-----------|
  | AI 엔진 | Python, PyTorch, Transformers, FastAPI |
  | 백엔드 | Java, Spring Boot, Gradle, JPA, HikariCP |
  | 프론트엔드 | (React 등 채우기) |
  | 익스텐션 | JavaScript (Chrome Extension MV3) |
  | 인프라 | AWS EC2 / RDS / ALB, Docker, GitHub Actions(CI/CD) |

  ---

  ## 📂 레포지토리 구조

  ```
  Capstone/
  ├── Advance-Capstone-AI/          # AI 분석 엔진
  ├── Advance-Capstone-Backend/     # Spring Boot 백엔드 (+ ai-engine 연동)
  ├── Advance-CapstoneFront_2-main/ # 웹 프론트엔드
  ├── Advanced-Capstone-ChromeExtension/ # 크롬 익스텐션
  └── src/                          # 감정분석 모델 학습/추론 파이프라인
  ```

  ---

  ## 🚀 실행 방법

  ### 1. 환경 변수 (`.env`)
  ```env
  YOUTUBE_API_KEY=...
  HUGGINGFACE_TOKEN=...
  GOOGLE_FACTCHECK_API_KEY=...
  OPENAI_API_KEY=...
  ```

  ### 2. AI 엔진 실행
  ```powershell
  & venv\Scripts\Activate.ps1
  python src/test_real_comments.py        # 유튜브 댓글 분석 파이프라인
  ```

  ### 3. 백엔드 실행
  ```bash
  cd Advance-Capstone-Backend
  ./gradlew bootRun
  # 또는 Docker
  docker-compose up
  ```

  ### 4. 프론트엔드 실행
  ```bash
  # (실행 명령 채우기)
  ```

  ---

  ## 🧠 AI 모델 상세

  - **감정 분석**: `klue/roberta-base` 파인튜닝, 라벨 `{0:부정, 1:긍정, 2:중립}`
    - 학습 데이터: NSMC 6,000개 + 수작업 댓글(뉴스/게임/예능) ×3 증강
    - 하이퍼파라미터: lr=2e-5, epochs=7, weight_decay=0.01
    - 배포: HuggingFace `thd011124/news-comment-sentiment`
  - **봇 탐지**: TF-IDF 유사도, 선동 키워드, 문장 길이 균일성 등 4종 신호

  ---

  ## 🛠️ 트러블슈팅 / 성능 개선 (어필 포인트)

  > 면접·발표에서 강조할 부분 — 본인이 한 일 중심으로

  - AI 응답 시간 **29초 → 14초** 단축 (프롬프트/파이프라인 최적화)
  - 백엔드 **N+1 쿼리 제거**, SQL 로깅 정리
  - **JMeter 부하 테스트** 기반 HikariCP 튜닝, MOCK_MODE 도입

  > 면접·발표에서 강조할 부분 — 본인이 한 일 중심으로

  - AI 응답 시간 **29초 → 14초** 단축 (프롬프트/파이프라인 최적화)
  - 백엔드 **N+1 쿼리 제거**, SQL 로깅 정리
  - **JMeter 부하 테스트** 기반 HikariCP 튜닝, MOCK_MODE 도입
  - (추가 항목 채우기)

  ---

  ## 👥 팀 구성 / 담당 역할

  | 이름 | 역할 | 담당 |
  |------|------|------|
  | 본인 | (예: AI/백엔드) | (담당 기능) |
  | 팀원 | | |

  ---

  ## 📄 관련 문서

  - [개발 계획서](./DEVELOPMENT_PLAN.md)
  - [API 명세](./Advance-Capstone-Backend/docs)
  - [ERD](./erd_article.html)
