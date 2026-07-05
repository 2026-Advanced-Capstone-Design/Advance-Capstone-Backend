# 측정 방법론 (Benchmark Method)

> 목적: 리팩토링의 성과 수치가 **어떤 환경에서 · 어떤 기준으로 · 어떻게** 측정된 것인지 명시한다.
> NHN클라우드 서면평가(2026-05-27)의 지적 — *"성과 수치가 학습 기반인지 운영 기반인지 모호", "검증 환경 불명확"* — 을 해소하기 위한 문서.
> **핵심 선언: 이 프로젝트의 모든 성능 수치는 학습 결과가 아니라 "운영 환경 부하 테스트(operational load test)" 기반이다.**

---

## 1. 측정 환경 (Environment)

측정 결과를 재현·해석하려면 환경을 고정·명시해야 한다. 결과 표에는 항상 아래 구성 중 어떤 것에서 측정했는지 라벨을 붙인다.

### 인스턴스 스펙
| 구성 | 노드 | 스펙 | 비고 |
|------|------|------|------|
| **단일 노드** | EC2 t2.micro ×1 | vCPU 1, RAM 1.9GB, 스왑 2GB | MySQL + backend + ai-engine + frontend + youtube-ai 동거 |
| **스케일아웃** | ALB + EC2 t2.micro ×2 | 각 노드 위와 동일 | EC2#1: MySQL+backend+ai-engine, EC2#2: backend+ai-engine |

- 리전: `ap-northeast-2`(서울)
- 컨테이너 오케스트레이션: `docker-compose.prod.yaml` (동일 파일 양 노드 공유)
- 부하 발생기: JMeter (로컬 PC → 인터넷 경유 EC2/ALB). **부하 발생기와 대상 서버는 분리**한다(같은 노드에서 부하를 만들지 않는다).

### AI 호출 모드 — MOCK_MODE (중요)
분석 파이프라인은 외부 LLM 호출이 지배적(1건 ~14초)이라, **순수 인프라·백엔드 처리 용량**과 **LLM 지연**을 분리 측정해야 한다.

- `MOCK_MODE=true`: ai-engine이 실제 LLM 호출 없이 즉시 응답 → **백엔드/DB/커넥션풀/네트워크 용량**(TPS·동시성·에러율) 측정용.
- `MOCK_MODE=false`: 실제 LLM 호출 포함 → **사용자 체감 지연(end-to-end)** 측정용.
- 설정 위치: `docker-compose.prod.yaml`의 ai-engine `MOCK_MODE: ${MOCK_MODE:-false}` / 코드 `Advance-Capstone-AI/ai-engine/routes/analyze.py`.
- **결과 표에는 반드시 MOCK_MODE 값을 함께 기재**한다 (혼동 방지).

---

## 2. 측정 지표 (Metrics)

### 부하 발생기 측(JMeter) — 사용자 관점
| 지표 | 정의 | 왜 보는가 |
|------|------|-----------|
| **에러율(%)** | 실패 응답 / 전체 요청 | 용량 한계의 1차 신호 (커넥션 고갈·타임아웃) |
| **TPS(req/s)** | 초당 처리 요청 수 | 처리량. 스케일아웃 효과의 핵심 지표 |
| **평균/p95/p99 응답시간(ms)** | 지연 분포 | 평균만 보면 꼬리 지연을 놓침 → **p95/p99 병기 필수** |

> 평균 대신 **p95/p99**를 대표값으로 쓴다. 소수의 느린 요청(꼬리)이 사용자 체감을 좌우하기 때문. actuator 히스토그램 버킷으로 서버 측에서도 동일 분위수를 재계산한다.

### 서버 관측 측(Prometheus + Grafana) — 내부 관점
동일 부하 구간에서 서버 내부를 함께 관측해 **"왜" 그 수치가 나왔는지** 설명한다. (대시보드: `NewsLens — Backend Overview`)

| 지표(Prometheus) | 패널 | 해석 포인트 |
|------------------|------|-------------|
| `http_server_requests_seconds`(히스토그램) | HTTP p95/p99 | JMeter p95와 교차검증 |
| `jvm_memory_used_bytes{area="heap"}` | JVM 힙 | 부하 중 힙 추세 → 메모리 누수/GC 압박 |
| `executor_queued_tasks`, `executor_active_threads` | 스레드풀 큐 깊이 | **백프레셔 발생 지점**(큐가 쌓이기 시작하는 동시성) |
| `ai_analyze_request_seconds`(히스토그램) | AI 호출 지연 | LLM 호출 병목 크기 |
| `cache_gets_total{result}` | 캐시 히트율 | 캐시 효과(중복 요청 절감) |
| HikariCP `hikaricp_connections_*` | (추가 패널 가능) | 커넥션 고갈 = 500명 에러의 직접 원인 규명 |

> **측정 도구 자체가 관측 대상에 영향**을 준다: Prometheus 15초 스크랩도 HTTP 요청으로 잡힌다. 부하 테스트 수치 해석 시 이 상수 트래픽을 감안한다.

---

## 3. 측정 절차 (Procedure)

1. **환경 고정**: 대상 구성(단일/스케일아웃)과 MOCK_MODE 값을 결정하고 기록.
2. **워밍업**: 본 측정 전 소규모 요청으로 JVM JIT/커넥션풀/캐시를 예열(cold start 왜곡 제거).
3. **기준선(baseline) 측정**: 개선 **전** 코드로 동일 시나리오 실행 → Grafana 스크린샷 + JMeter 리포트 저장.
4. **개선 적용** 후 **동일 시나리오** 재실행 → before/after 같은 축으로 비교.
5. **3회 이상 반복**하여 이상치 제외, 중앙값 사용(단발 측정 신뢰 금지).
6. 각 실행마다 **Grafana 시간범위를 해당 구간으로 고정 캡처** → 문서/발표 자료화.

### 표준 시나리오 (JMeter 플랜)
| 플랜 파일 | 동시 사용자 | Ramp-up | 루프 | 총 요청 |
|-----------|-------------|---------|------|---------|
| `jmeter/news_analyze_100users.jmx` | 100 | 30s | 10 | 1,000 |
| `jmeter/news_analyze_500users.jmx` | 500 | 60s | 10 | 5,000 |
| `jmeter/news_analyze_1000users.jmx` | 1000 | 60s | 10 | 10,000 |
| 실행 스크립트 | `jmeter/run_load_test.ps1` (로컬) / `jmeter/ec2_setup_mock.sh` (EC2 MOCK 설정) | | | |

---

## 4. 기준선 결과 (Baseline, 기록 보존)

> 재측정 시 이 표를 갱신하지 말고 **새 행을 추가**해 이력을 남긴다.

| 일자 | 구성 | 동시 | MOCK | 에러율 | 평균(ms) | TPS |
|------|------|------|------|--------|----------|-----|
| 2026-05-21 | 단일 t2.micro | 100 | true | 0.00% | 100 | 33.4 |
| 2026-05-21 | 단일 t2.micro | 500 | true | **8.58%** | 103 | 78.7 |
| 2026-05-25 | ALB + EC2 2대 | 1000 | true | 0.00% | 27,740* | 82.7 |

\* 1000명/2노드 평균 응답 27.7s는 **대기열 포화 구간**(수용량 초과 시 지연 급증)을 의미. TPS는 유지되나 지연이 튀는 전형적 형태.

**해석 요지**
- 단일 노드 한계 = 100명 안정 / 500명에서 에러율 급증.
- 500명 에러 원인 = **HikariCP 커넥션 고갈(COMMON_999)** — pool 30으로도 부족. → 이후 Grafana `hikaricp_connections_pending` 패널로 재현·입증 예정.
- ALB 2노드로 1000명 무에러 처리 → **수평 확장으로 처리량 확보**.

---

## 5. 결과 보고 규칙 (신뢰도 원칙)

- 수치에는 **항상 4종 컨텍스트를 붙인다**: `구성 / 동시 사용자 / MOCK_MODE / 측정일`.
- **평균 단독 금지** — p95/p99를 함께 보고.
- "빨라졌다" 대신 **"X 구성·Y 부하에서 p95 A→B (n회 중앙값)"** 형태로 서술.
- JMeter 수치와 Grafana 서버 지표를 **교차검증**해 한쪽만으로 결론 내지 않는다.
- 실패·이상치도 [troubleshooting.md](troubleshooting.md)에 남긴다(성공 사례만 보고 금지).

---

## 6. 관련 문서
- 모니터링 스택 의사결정: [adr/002-monitoring-stack.md](adr/002-monitoring-stack.md)
- 시행착오 기록: [troubleshooting.md](troubleshooting.md)
- 리팩토링 전체 계획: `../../REFACTORING_PLAN.md`
