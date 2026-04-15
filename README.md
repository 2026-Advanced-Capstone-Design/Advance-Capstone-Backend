# 📅 주차별 개발 계획

> **마지막 수정**: 2026-04-16  
> **변경 사항**: 7주차부터 Fine-tuning → **Prompt Engineering** 방식으로 전환  
> Fine-tuning 대신 GPT-4o 기반 프롬프트 엔지니어링 + ChromaDB RAG 파이프라인으로 대체

---

## ✅ 완료

### 1~3주차: 기획 및 설계

- [x] 프로젝트 주제 선정 및 기능 구상
- [x] 유사 서비스 분석 (AllSides, Ground News)
- [x] 시스템 아키텍처 설계 (4-Layer)
- [x] 기술 스택 선정 및 근거 정리

---

### 4주차 (3/26 ~ 4/1): 구조 발표 + AI 실험

- [x] Week 4 프로젝트 구조 발표 (12장 PPT)
- [x] AI 모델 비교 실험 (5개 모델, 샘플 68건 — 코드 검증)
- [x] DB 스키마 설계 (10개 테이블)
- [x] 요구사항 명세서 작성
- [x] GitHub PR 템플릿 · 컨벤션 문서 작성

---

### 5주차 (4/2 ~ 4/8): 백엔드 기반 세팅 + AI 본격 실험

#### 백엔드
- [x] Docker Compose로 MySQL + Redis 로컬 환경 구축
- [x] application.yml 설정 (local/prod 분리)
- [x] JPA 엔티티 10개 생성 + 테이블 자동 생성 확인
- [x] Spring Boot 정상 실행 확인
- [x] Swagger 설정 + API stub 작성
- [x] GitHub 레포 생성 + develop 브랜치 + 팀원 공유

#### AI (Colab)
- [x] NSMC 10,000건으로 5개 모델 본격 비교 실험
- [x] 실험 결과 정리 + 최종 모델 확정

> 🎯 **마일스톤 M1**: Spring Boot 실행 + Swagger + GitHub 공유 완료

---

### 6주차 (4/9 ~ 4/15): API 뼈대 + 라벨링 시작

#### 백엔드
- [x] 뉴스 분석 요청 API (`POST /api/v1/articles/analyze/text|url|image`)
- [x] 분석 결과 조회 API (`GET /api/v1/articles/{id}/result`)
- [x] 분석 상태 조회 API (`GET /api/v1/articles/{id}/status`)
- [x] 피드백 API (`POST /api/v1/feedbacks`)
- [ ] 사용자 인증 (회원가입 / 로그인 / JWT) — **보류**
- [ ] GlobalExceptionHandler + 통일 응답 포맷

#### AI
- [x] 라벨링 데이터 수집 시작 (BigKinds 뉴스 기사)
- [x] LLM 보조 라벨링 (GPT-4o-mini 초벌 → 사람 검수)
- [x] 데이터 전처리 파이프라인 품질 검증
- [x] 프롬프트 엔지니어링 기법 연구 및 방향 확정

> 🎯 **마일스톤 M2**: API 완성 → Swagger 문서 공유 → 프론트팀 개발 시작 가능

---

## 🔄 진행 중

### 7주차 (4/16 ~ 4/22): Flask 서버 구축 + 프롬프트 엔지니어링 시작

> **핵심 변경**: Fine-tuning 제거 → GPT 프롬프트 엔지니어링으로 전환  
> 라벨 데이터 3,000건은 Few-shot 예시 선별 + 품질 검증용으로 활용

#### 백엔드
- [ ] `GlobalExceptionHandler` + 통일 응답 포맷 완성
- [ ] Spring 정규식 기반 입력 전처리 (URL 유효성, 텍스트 길이 등)
- [ ] Spring → Flask 비동기 호출 기본 구조 (`AiWorkerClient`)

#### AI (Flask)
- [ ] Flask 서버 프로젝트 세팅 (라우터 `/analyze`, `/status`)
- [ ] 전처리 파이프라인 구현 (BeautifulSoup + KoNLPy + 정규식)
- [ ] **Zero-shot + Constraint** 프롬프트로 요약 / 키워드 생성 (GPT-4o-mini)
- [ ] ChromaDB 연동 + `ko-sroberta-multitask` 임베딩 저장

---

## 📋 예정

### 8주차 (4/23 ~ 4/29): 핵심 분석 프롬프트 구현

#### AI (Flask)

| 기법 | 설명 |
|------|------|
| **Few-shot Prompting** | 3,000건 라벨 중 confidence 0.9↑ 예시 선별 → 진보/보수/중립 각 1건 이상 |
| **Generated Knowledge** | GPT에게 이슈 배경 지식 먼저 생성하게 한 뒤 편향 분석에 활용 |
| **Chain of Thought (CoT)** | 어휘 선택 → 프레이밍 → 인용 편향 → 정보 생략 4단계 순차 추론 |
| **Prompt Merging** | 배경 지식 생성 + 편향 검증을 1개 프롬프트로 통합 (API 호출 수 절감) |
| **Model Tiering** | 배경 지식 생성: GPT-4o-mini / 최종 검증: GPT-4o |

- [ ] Few-shot 편향 분류 프롬프트 작성 및 검증
- [ ] Generated Knowledge Prompting 구현
- [ ] CoT 편향 분석 4단계 프롬프트 구현
- [ ] Prompt Merging 적용 (배경 지식 + 검증 통합)
- [ ] Model Tiering 분기 로직 구현

#### 백엔드
- [ ] Docker Compose에 Flask 서버 + ChromaDB 서비스 추가
- [ ] Spring ↔ Flask 연동 통합 테스트

> 🎯 **마일스톤 M3**: 텍스트 입력 → AI 분석 → 결과 반환 파이프라인 동작

---

### 9주차 (4/30 ~ 5/6): RAG + 비교 분석 + 크롤링/OCR

#### AI (Flask)

| 기법 | 설명 |
|------|------|
| **Topic-based Caching** | 이슈별 배경 지식을 DB에 저장, 같은 이슈 재요청 시 GPT 호출 없이 재사용 |
| **RAG** | ChromaDB 코사인 유사도로 유사 기사 top-k 검색 (의미 기반, 키워드 매칭 X) |
| **Constraint Prompting** | 비교 분석 시 "어느 쪽이 옳다고 판단하지 마" + "본문 1,500자 제한" 이중 제약 |
| **Role Prompting** | 편향 리포트 생성 시 "뉴스 리터러시 교육 전문가" 역할 부여 |

- [ ] Topic-based Caching 구현 (이슈별 배경 지식 저장 / 조회)
- [ ] RAG 파이프라인 완성 (ChromaDB 코사인 유사도 top-k 검색)
- [ ] 유사 기사 비교 분석 프롬프트 구현
- [ ] 편향 리포트 생성 프롬프트 구현

#### 백엔드
- [ ] `NewsCrawlerService` 구현 (Jsoup, 언론사별 파서)
- [ ] `OcrService` 구현 (Tess4J + Clova OCR fallback)
- [ ] `PreprocessService` 구현 (정규식 + 문장 분리)
- [ ] input_type별 분기 처리 완성 (TEXT / IMAGE / URL)

> 🎯 **마일스톤 M4**: 전체 분석 파이프라인 완성 (RAG + 비교 분석 포함)

---

### 10주차 (5/7 ~ 5/13): 점수 집계 + 캐싱 + 성능 최적화

#### 백엔드
- [ ] `BiasScoreCalculator` 구현 (4대 지표 가중 합산 → `total_score`)
- [ ] 문장별 분석 결과 저장 (`sentence_analyses`, 하이라이팅 판정)
- [ ] Redis 캐싱 구현 (URL 해시 기반, TTL 30분)
- [ ] Clova OCR 무료 한도 카운터 (Redis)
- [ ] API Rate Limiting
- [ ] N+1 쿼리 최적화 (fetch join)

#### 통합
- [ ] 프론트 ↔ 백엔드 첫 연동 테스트

> 🎯 **마일스톤 M5**: 전체 기능 완성 (점수 + 캐싱 + RAG)

---

### 11주차 (5/14 ~ 5/20): 배포 + CI/CD

- [ ] AWS EC2 세팅 (t3.medium, Swap 2GB)
- [ ] Docker Compose 프로덕션 설정
- [ ] Nginx 리버스 프록시 + HTTPS
- [ ] GitHub Actions CI/CD (develop → 빌드+테스트 / main → 배포)
- [ ] 환경변수 관리 (`.env`)

> 🎯 **마일스톤 M6**: EC2에서 전체 서비스 동작 + 자동 배포

---

### 12주차 (5/21 ~ 5/27): 통합 테스트 + QA

- [ ] End-to-End 테스트 (입력 → 분석 → 결과 시각화)
- [ ] 크롬 확장 프로그램 연동 테스트
- [ ] 에지 케이스 테스트 (빈 텍스트, 잘못된 URL, OCR 실패 등)
- [ ] 부하 테스트 (JMeter, 동시 10건)
- [ ] 프롬프트 정확도 평가 (3,000건 라벨 데이터 기준 검증)
- [ ] 버그 수정 + 성능 개선

> 🎯 **마일스톤 M7**: QA 완료, 버그 없는 상태

---

### 13주차 (5/28 ~ 6/3): 마무리 + 문서화

- [ ] 최종 버그 수정 + 코드 리팩토링
- [ ] README 최종 정리 (설치 가이드, 실행 방법)
- [ ] API 문서 최종 정리 (Swagger)
- [ ] 최종 보고서 작성
- [ ] 실험 결과 정리 (프롬프트 기법별 성능 비교, 부하 테스트)

---

### 14주차 (6/4 ~ 6/8): 발표 준비

- [ ] 최종 발표 PPT 제작 (15~20장)
- [ ] 발표 스크립트 작성
- [ ] 데모 시나리오 준비 (실시간 뉴스 분석 시연)
- [ ] 발표 리허설 2~3회
- [ ] 최종 보고서 제출

---

### 15주차 (6/9 ~ 6/10): 최종 발표 🎓

- [ ] 6/10 최종 발표
- [ ] 질의응답 대비 예상 질문 정리
- [ ] 시연 환경 최종 점검

---

## 🏆 마일스톤 요약

| 마일스톤 | 기한 | 체크포인트 | 상태 |
|---------|------|-----------|------|
| M1 | 5주차 (4/8) | Spring Boot 실행 + Swagger + GitHub 공유 | ✅ 완료 |
| M2 | 6주차 (4/15) | API 완성 → 프론트 개발 시작 가능 | ✅ 완료 |
| M3 | 8주차 (4/29) | 텍스트 → AI 분석 → 결과 반환 파이프라인 동작 | ⬜ 예정 |
| M4 | 9주차 (5/6) | 전체 분석 파이프라인 완성 (RAG + 비교 분석) | ⬜ 예정 |
| M5 | 10주차 (5/13) | 전체 기능 완성 (점수 + 캐싱 + RAG) | ⬜ 예정 |
| M6 | 11주차 (5/20) | EC2 배포 + CI/CD 자동화 | ⬜ 예정 |
| M7 | 12주차 (5/27) | QA 완료 | ⬜ 예정 |
| M8 | 15주차 (6/10) | 최종 발표 | ⬜ 예정 |

---

## 🔀 기존 계획 대비 주요 변경 사항

| 항목 | 기존 (파인튜닝) | 변경 (프롬프트 엔지니어링) |
|------|--------------|--------------------------|
| 분석 방식 | klue/roberta-base 파인튜닝 | GPT-4o 프롬프트 엔지니어링 |
| 라벨 데이터 용도 | 모델 학습 데이터 | Few-shot 예시 선별 + 품질 검증 |
| 7~8주차 AI 작업 | 파인튜닝 3개 태스크 | Flask 서버 + 핵심 프롬프트 구현 |
| 유사 기사 검색 | 미정 | ChromaDB RAG (ko-sroberta-multitask) |
| API 비용 최적화 | 없음 | Prompt Merging + Topic Caching + Model Tiering |
| 추론 속도 | 빠름 (roberta ~0.3초) | 느림 (GPT ~3~10초) → 비동기 폴링으로 보완 |

### 프롬프트 엔지니어링 기법 적용 위치

```
BigKinds 기사 수집
    └─ 전처리 파이프라인 (BeautifulSoup + KoNLPy)
        └─ [Zero-shot + Constraint] 요약 / 키워드 생성 (GPT-4o-mini)
            └─ ChromaDB 저장 (ko-sroberta-multitask 임베딩)

분석 요청 수신
    └─ [Generated Knowledge] 이슈 배경 지식 생성
        └─ [Topic-based Caching] 동일 이슈면 DB에서 재사용
            └─ [Few-shot + CoT] 편향 분석 4단계
                └─ [Prompt Merging] 배경 지식 + 검증 통합 (호출 수 절감)
                    └─ [Model Tiering] mini(배경) / 4o(검증) 분기
                        └─ [Role Prompting] 편향 리포트 생성
                            └─ [RAG] ChromaDB 유사 기사 검색
                                └─ [Constraint] 비교 분석 (판단 금지 + 1500자 제한)
```
