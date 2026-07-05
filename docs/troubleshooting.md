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

---

## 2026-07-05 — Phase 7: `@Transactional` 프록시 자기호출 함정

### 사례 5 — 붙어 있어도 동작하지 않던 `@Transactional`

#### 1) 문제 발견
- `ArticleService.saveImageArticle`에 `@Transactional`이 붙어 있는데, 실제로는 트랜잭션 애노테이션이 **적용되지 않는** 상태였다. (2026-07-03 코드 감사에서 발견, 문제 D)
- 근거: `ArticleService.submitImage`(`:128`, `@Transactional`)가 같은 클래스의 `saveImageArticle`(`:151-152`, `@Transactional protected`)를 `saveImageArticle(...)`로 **직접(자기)호출**.

#### 2) 원인 분석
1. Spring의 `@Transactional`은 **AOP 프록시**로 동작한다. 빈을 주입받아 **외부에서** 메서드를 호출하면 프록시가 가로채 트랜잭션을 시작하지만, **같은 클래스 안에서 `this.method()`로 자기호출하면 프록시를 거치지 않아** 애노테이션이 무시된다.
2. `submitImage`(`@Transactional`) → **자기호출** `saveImageArticle`(`@Transactional protected`) 구조라, `saveImageArticle`의 애노테이션은 무효.
3. 지금까지 버그로 드러나지 않은 이유: 호출자 `submitImage`가 이미 `@Transactional`이라, `saveImageArticle`의 `save()`가 **상위 트랜잭션에 편승**해 정상 커밋됐기 때문(잠복). 만약 `saveImageArticle`를 프록시 없이 단독으로 쓰거나, 다른 전파 옵션(`REQUIRES_NEW` 등)을 기대했다면 그 순간 조용히 깨졌을 것.

#### 3) 해결법 파악
- 선택지: (a) 별도 빈으로 분리, (b) 자기 자신을 주입(self-injection), (c) `AopContext.currentProxy()`, (d) **애노테이션 제거 + `private` 헬퍼로 정리**.
- 이 메서드는 **별도 트랜잭션 경계가 필요 없다**(호출자의 tx에 편승하는 게 의도된 동작). 따라서 (a)~(c)는 과설계. **(d)** 가 정답 — 거짓 안전감(false confidence)을 주는 무효 애노테이션을 걷어내고, "호출자 트랜잭션 안에서 실행되는 헬퍼"임을 코드로 드러낸다. 호출부가 `submitImage` 하나뿐임을 grep으로 확인해 `private` 격하가 안전함을 검증.

#### 4) 해결법 적용 및 확인
- 적용:
  ```java
  // before
  @Transactional
  protected Article saveImageArticle(String imagePath) { ... }
  // after (private 헬퍼 + 애노테이션 제거)
  private Article saveImageArticle(String imagePath) { ... }
  ```
- 확인: `./gradlew compileJava` 성공(무관한 deprecation 경고만). 동작은 이전과 동일(상위 tx 편승 유지)이라 계약 변화 없음.

#### 교훈
- **`@Transactional`(및 `@Async`, `@Cacheable` 등 모든 프록시 기반 애노테이션)은 자기호출에서 무효다.** 별도 트랜잭션 경계가 필요하면 다른 빈 분리 / self-injection / `AopContext`를 써야 하고, 필요 없으면 애노테이션을 제거해 오해를 없애야 한다.
- 이 감별은 **Phase 8**(트랜잭션 경계 축소)에서 `REQUIRES_NEW`로 상태 쓰기를 분리할 때 직접 재사용된다 — 거기선 반대로 "경계가 필요한" 경우라 별도 빈으로 뽑는다.

---

## 2026-07-05 — Phase 8: 외부 HTTP 호출을 트랜잭션 밖으로 (HikariCP 고갈 근본원인)

### 사례 6 — HTTP 응답을 기다리는 내내 DB 커넥션을 물고 있던 트랜잭션

#### 1) 문제 발견
- 과거 부하 테스트에서 HikariCP가 반복적으로 고갈(`Connection is not available, request timed out`)돼 pool 크기를 30까지 올리며 튜닝했지만 근본 해결이 안 됐다. 2026-07-03 코드 감사에서 **진짜 원인**을 특정(문제 C).
- 근거: `AiWorkerClient.submitAnalysis`(`@Async("aiWorkerExecutor")` + **`@Transactional`**)가 메서드 전체를 하나의 트랜잭션으로 감싼 채, 그 안에서 AI 엔진에 `/analyze` **HTTP 요청**을 보낸다.

#### 2) 원인 분석
- Spring 트랜잭션은 **시작 시 HikariCP 커넥션 1개를 획득해 커밋/롤백 전까지 반납하지 않는다.** `submitAnalysis`는 메서드 전체가 tx라 타임라인이 다음과 같았다:
  ```
  tx 시작 → 커넥션 획득 🔒
    ① UPDATE status=ANALYZING   (수 ms, DB 작업)
    ② AI /analyze HTTP 호출      (수 초, DB 작업 없음 — 그런데도 커넥션 점유)
  tx 커밋 → 커넥션 반납 🔓
  ```
- ②번 HTTP 대기 구간엔 DB 작업이 전혀 없는데도 커넥션을 쥐고 있었다. `aiWorkerExecutor`가 최대 8스레드 → 부하 시 **8개 커넥션이 HTTP 응답만 기다리며** 장시간 점유 → 조회 요청(`getStatus`/`getResult`)이 커넥션을 못 받아 전면 지연/타임아웃.
- **즉 지난 "pool을 30으로 늘리는" 튜닝은 밑 빠진 독이었다.** HTTP가 느려지면 커넥션이 HTTP 시간만큼 묶이는 구조 자체가 문제였으므로.

#### 3) 해결법 파악
- 방향: **트랜잭션 경계를 "상태 UPDATE 한 줄"로 축소하고, HTTP 호출은 트랜잭션 밖에서 수행.**
- 구현 갈림길: 상태 UPDATE를 짧은 tx로 만들려면 그 자리에서 커밋돼야 하는데, **같은 클래스에 tx 메서드를 만들어 자기호출하면 Phase 7에서 본 대로 프록시 우회로 무효**가 된다. → 반드시 **다른 빈**으로 분리해 프록시를 경유하게 해야 함.
- 전파 옵션: `submitAnalysis`에서 `@Transactional`을 제거하면 바깥 tx가 없어 기본 전파(`REQUIRED`)로도 새 tx가 생기지만, **`REQUIRES_NEW`를 명시**해 "독립된 짧은 tx"라는 의도를 코드로 못박고 향후 호출부에 tx가 생겨도 안전하게 한다.

#### 4) 해결법 적용 및 확인
- 신설 `ArticleStatusWriter` 빈(상태 변경 전담, `@Transactional(REQUIRES_NEW)`):
  ```java
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateStatus(Long articleId, ArticleStatus status) {
      articleRepository.updateStatus(articleId, status);
  }
  ```
- `AiWorkerClient` 변경: 메서드의 `@Transactional` **제거**, 상태 쓰기를 `statusWriter.updateStatus(...)`로 위임(ANALYZING/FAILED 두 곳). HTTP 호출은 트랜잭션 밖으로 밀려남. 불필요해진 `ArticleRepository` 의존/`private updateStatus` 헬퍼 삭제.
  ```
  updateStatus(ANALYZING) → 짧은 tx 🔒🔓 (커넥션 즉시 반납)
  AI /analyze HTTP 호출     → 커넥션 없이 대기
  (실패 시) updateStatus(FAILED) → 짧은 tx 🔒🔓
  ```
- 확인(정적): `./gradlew compileJava` 성공(EXIT=0). `article`의 스칼라 필드 접근은 detached 상태에서도 안전(원래도 `@Async`라 caller tx 미전파)이라 계약/동작 불변.
- 확인(부하, **예정**): JMeter 부하 중 Grafana `hikaricp_connections_active`/`hikaricp_connections_pending` **before/after** 비교. AI 엔진을 `MOCK_DELAY_SECONDS`로 느리게 해 재현 → before는 active가 8에 붙고 pending 급증, after는 active 낮게 유지 예상. (Phase 2 세마포어 시연과 동일 방식)

#### 교훈
- **트랜잭션 안에서 외부 I/O(HTTP/외부 API)를 호출하지 마라.** 커넥션은 "DB를 실제로 만지는 순간에만" 잡아야 한다. 느린 외부 호출을 tx로 감싸면 pool 크기를 아무리 키워도 동시성 한계에서 고갈된다.
- HikariCP 고갈은 대개 "pool이 작아서"가 아니라 **"커넥션을 너무 오래 쥐고 있어서"**다. 튜닝 전에 **커넥션 점유 시간**을 먼저 의심할 것.

---

## 2026-07-05 — Phase 9-1: 콜백 멱등성 (중복 콜백 → 결과 중복 → 500)

### 사례 7 — 같은 콜백이 두 번 오면 조회가 500나던 문제

#### 1) 문제 발견
- AI 엔진이 분석 완료를 백엔드 `/api/v1/internal/callback`으로 **fire-and-forget 푸시**하는데, 이 콜백이 **중복 도착**하면 `analysis_results`에 결과 행이 2개 쌓이고, 이후 결과 조회가 `NonUniqueResultException`(500)으로 터진다. (2026-07-03 감사, 문제 A1)
- 근거: `AnalysisCallbackService.handleCallback`이 진입 즉시 **무조건** `analysisResultRepository.save(result)` / `AnalysisResult.article`의 `@JoinColumn`에 `unique` 없음 / 조회 `findByArticleId`는 단건 `Optional` 기대.

#### 2) 원인 분석
- 분산 시스템에서 네트워크 전달은 **exactly-once가 불가능**하다. 콜백 응답 지연·네트워크 중복·(추후 붙일) 재시도 때문에 **at-least-once**(한 번 이상)로 도착할 수 있다.
- 그런데 받는 쪽 `handleCallback`이 **멱등하지 않다** — 올 때마다 결과를 새로 INSERT. "같은 요청을 여러 번 처리해도 결과가 한 번과 같아야 한다"는 멱등성이 깨져 있음.
- DB에도 방어선이 없음(`ARTICLE_ID` UNIQUE 부재) → 중복행이 물리적으로 허용됨 → 단건 조회가 깨짐.

#### 3) 해결법 파악
- **방어 심층(defense-in-depth) 2겹**으로 설계:
  1. **DB 레벨(최후 방어선)**: `analysis_results.ARTICLE_ID`에 **UNIQUE** — 로직을 뚫어도 두 번째 INSERT를 DB가 거부. 동시 콜백 2건이 가드를 동시에 통과하는 극단 경합도 여기서 차단(둘 중 하나는 제약 위반으로 롤백, 결과는 1건 유지).
  2. **앱 레벨(정상 흐름 예방)**: `handleCallback` 진입 시 **"이미 결과가 존재하면 무시하고 리턴"** 하는 멱등 가드. 500을 애초에 안 만들고 조용히 no-op.
- 왜 "결과 존재"를 신호로? 상태값(`DONE`)보다 **결과 행의 존재**가 "성공 콜백이 이미 처리됨"의 더 직접적인 증거라 판단(상태 경합=A4와 독립적으로 견고).

#### 4) 해결법 적용 및 확인
- `AnalysisResult.java`: `@JoinColumn(name = "ARTICLE_ID")` → **`unique = true`** 추가.
- `AnalysisResultRepository.java`: 멱등 가드용 `existsByArticleId(Long)` 쿼리 추가(`SELECT COUNT(r) > 0 ...`).
- `AnalysisCallbackService.handleCallback`: `findById` 직후 **가드 삽입** — `existsByArticleId`면 로그 남기고 `return`(FAILED 분기보다 앞이라, 늦게 온 FAILED가 이미 DONE을 덮지도 못함).
- 확인(정적): `./gradlew compileJava` 성공(EXIT=0).
- 확인(동작, **예정**): 같은 콜백 바디를 2회 POST → 1회차 정상 저장/DONE, 2회차 "중복 콜백 무시" 로그 + 결과 1건 유지 + 조회 200. Micrometer 카운터(중복 콜백 수)로 관측 예정.
- ⚠️ **운영 주의(migration)**: 현재 스키마는 Flyway 없이 `ddl-auto: update`. Hibernate `update`는 **기존 MySQL 테이블의 컬럼에 UNIQUE 제약을 자동 추가하지 못할 수 있다**(새로 만드는 테이블엔 적용). 이미 운영 중인 DB에는 수동 DDL 필요:
  ```sql
  ALTER TABLE analysis_results ADD CONSTRAINT uk_analysis_results_article UNIQUE (ARTICLE_ID);
  -- 기존 중복행이 있으면 제약 추가 전에 정리 필요
  ```

#### 교훈
- **네트워크 경계를 넘는 콜백/웹훅 수신부는 항상 멱등하게 설계한다.** "정확히 한 번 온다"는 가정은 분산 환경에서 성립하지 않는다.
- 멱등성은 **앱 가드 + DB 제약을 함께** 두는 게 정석. 앱 가드는 정상 흐름의 500을 없애고, DB 제약은 동시성 경합·로직 버그까지 막는 최후 방어선.
- 다음(9-5, AI 콜백 재시도)을 붙이면 중복 확률이 **올라가므로**, 멱등성(9-1)을 먼저 깐 순서가 맞다.
