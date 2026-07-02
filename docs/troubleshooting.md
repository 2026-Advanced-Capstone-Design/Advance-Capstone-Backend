# 트러블슈팅 로그

> 리팩토링 중 만난 문제·원인분석·시행착오를 시간순으로 기록한다.
> 성공만이 아니라 막다른 길과 오인(誤認)까지 남겨, 발표/이력서의 신뢰도를 높이는 것이 목적.

---

## 2026-07-02 — Phase 1 모니터링 스택 EC2 배포 및 지표 검증

### 사례 1 — 모니터링 스택이 "클라우드에서 안 떴다"고 여긴 문제

- **문제**: 지난 세션에 만든 모니터링 스택을 EC2에 올렸는데 Grafana 대시보드가 동작하지 않는다고 인식.
- **원인분석**:
  1. `docker ps` 확인 결과 `prometheus`, `grafana` 컨테이너는 **"Up 22 hours"** — 이미 정상 기동 중이었음.
  2. `curl localhost:9090/api/v1/targets` → `"health":"up"` — 백엔드 스크랩도 **처음부터 정상**이었음.
  3. 진짜 원인은 두 가지: **(a) 보안그룹에 포트 3000(Grafana) 미개방으로 브라우저 접속 불가**, **(b) `simple` 스택은 자동 프로비저닝이 없어 대시보드를 수동 임포트해야 함**.
- **해결**: 보안그룹 인바운드에 TCP 3000 추가 → Grafana 접속 → Prometheus 데이터소스(`http://prometheus:9090`) 연결 → `newslens-overview.json` 임포트.
- **교훈**: "안 된다"의 체감 원인과 실제 원인이 다를 수 있다. **컨테이너 상태 → 스크랩 health → 접근 경로** 순으로 계층을 분리해 확인해야 한다.

### 사례 2 — 도커 네트워크가 2개라 스크랩 대상이 헷갈림

- **문제**: `docker network ls`에 `newslens_newslens-network`와 `ubuntu_newslens-network` **두 개**가 존재. 모니터링 compose를 어느 쪽에 붙여야 백엔드를 스크랩하는지 불명확.
- **원인분석**: Docker Compose는 네트워크 이름에 **프로젝트명(=실행 디렉토리명) 접두어**를 붙인다. 앱을 `~/newslens/`에서 띄우면 `newslens_...`, 홈 `~/`에서 띄우면 `ubuntu_...`가 생성됨. 과거에 두 위치에서 각각 실행한 잔재로 네트워크가 둘 생겼다.
- **해결**: `docker inspect newslens-backend --format '{{range ...Networks}}...'`로 백엔드 실제 소속 확인 → **`newslens_newslens-network`**. 모니터링 compose의 `networks.default.name`을 이 값으로 맞춤(이미 일치). 같은 네트워크 안에서는 컨테이너명(`newslens-backend`)으로 스크랩 대상 해석됨.
- **교훈**: external 네트워크 참조 시 **compose 네트워크 이름 = `<프로젝트명>_<네트워크명>`** 규칙을 기억할 것. 이름 불일치 시 `network ... declared as external, but could not be found` 에러.

### 사례 3 — 메모리 부족 → 풀 스택 대신 경량 스택 분리

- **문제**: t2.micro(1.9GB RAM). 앱 스택만으로 여유가 수십 MB. 풀 모니터링 스택(Prometheus+Grafana+Loki+promtail+node-exporter+cAdvisor, 총 ~1GB)은 올릴 수 없음.
- **원인분석**: 모니터링 컨테이너 6개의 합산 메모리가 호스트 예산을 초과. (메모리는 **compose 파일 단위가 아니라 컨테이너/호스트 단위**로 소비됨 — 파일을 나눈다고 메모리가 늘지 않는다.)
- **해결**: `monitoring/simple/`에 **Prometheus + Grafana 2개만** 정의한 경량 compose를 별도 파일로 만들어 배포. 각 컨테이너 `mem_limit: 256m`. 스왑 2GB 병행. 로그 수집(Loki) 및 호스트/컨테이너 지표(node-exporter/cAdvisor)는 메모리 여유 확보 후로 유예.
- **교훈**: compose를 나눈 실익은 "메모리 분리"가 아니라 **① 앱과 독립적으로 start/stop ② 더 가벼운 부분집합만 선택 기동**. 자원 절약은 "띄우는 컨테이너 수를 줄여서" 얻는 것.

### 사례 4 — 대시보드 지표가 "0"이라 고장으로 오인 (가장 큰 시행착오)

- **문제**: 스레드풀 큐·캐시 히트율·AI 지연 패널이 비어 보임. 처음엔 `application="factcheck"` 라벨 필터가 잘못됐다고 판단하고 쿼리를 `job="backend"`로 바꾸려 함.
- **원인분석**: `curl localhost:8080/actuator/prometheus`로 실제 노출 지표를 직접 확인한 결과:
  ```
  ai_analyze_request_seconds_count{application="factcheck",outcome="success"} 3
  executor_completed_tasks_total{name="aiWorkerExecutor"} 3.0
  cache_gets_total{...,result="miss"} 3.0   /  {...,result="hit"} 0.0
  executor_queued_tasks{name="aiWorkerExecutor"} 0.0
  ```
  → **`application` 라벨은 원래부터 정상 존재**했고 값이 실제로 0/미발생이었을 뿐. 라벨 필터는 문제가 아니었다.
  - **스레드풀 큐=0**: core 4스레드라 동시 5건 미만이면 큐가 안 쌓임 → 0이 정상.
  - **캐시 hit=0**: 매번 다른 기사라 전부 miss(3 miss) → hit 비율 0이 정상.
  - **AI 지연 No data**: `histogram_quantile(rate(..._bucket[5m]))`은 **최근 5분 내 분석**이 있어야 값 생성. HTTP p95는 15초 스크랩 자체가 트래픽이라 항상 뜨지만, `ai_analyze_request`는 실제 분석 시에만 증가.
- **해결**: 오인이었음을 확인하고 라벨 필터는 `application`/`job` 무엇이든 무방(둘 다 존재)로 정리. AI 패널은 분석을 몇 건 실행하니 즉시 p95가 그려짐. 뜸한 트래픽 대비 `[5m]`→`[10m]` 창 확대 권장.
- **교훈**: **"지표가 존재하면 쿼리 수정으로, 존재하지 않으면 쿼리로는 절대 못 뜬다."** 대시보드가 비었을 때 쿼리부터 고치지 말고 **`/actuator/prometheus` 원본을 grep해 지표·라벨·값을 먼저 확인**할 것. "0"과 "no-data(NaN)"와 "라벨 불일치"는 원인이 전혀 다르다.

---

### 이 세션에서 확정된 사실 (요약)
- 백엔드 커스텀 지표 정상: `ai_analyze_request_*`(Timer), `executor_*`(ThreadPoolTaskExecutor 자동), `cache_gets_total`(Caffeine recordStats).
- 라벨: `application="factcheck"`와 `job="backend"` 모두 유효.
- 대시보드 6패널 중 5개 실데이터 검증(에러 로그율만 Loki 미배포로 의도적 no-data).
