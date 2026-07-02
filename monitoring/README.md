# 모니터링 스택 (Phase 1)

Prometheus + Grafana + Loki 기반 자체호스팅 모니터링/로그 스택.

## 기동

```bash
# 1) 앱 스택이 먼저 떠 있어야 함 (newslens-network 생성 목적)
docker compose -f docker-compose.prod.yaml up -d

# 2) 모니터링 스택 기동
docker compose -f monitoring/docker-compose.monitoring.yaml up -d
```

접속:
- Grafana: http://<host>:3000  (기본 admin / admin — `GRAFANA_PASSWORD` 환경변수로 변경)
- Prometheus: http://<host>:9090
- cAdvisor: http://<host>:8081

## 자동으로 되는 것 (provisioning)

Grafana 시작 시 아래가 **자동 등록**된다. UI 클릭 불필요.
- **데이터소스**: Prometheus, Loki (`grafana/provisioning/datasources/`)
- **대시보드**: `grafana/dashboards/*.json` → "NewsLens" 폴더에 자동 로드
  - `newslens-overview.json`: HTTP p95/p99, JVM 힙, 스레드풀 큐 깊이, AI 호출 지연, 캐시 히트율, 에러 로그율

## 수동으로 하는 것 (표준 대시보드 import)

JVM/노드/컨테이너 표준 대시보드는 JSON이 방대하므로 **커뮤니티 대시보드 ID로 import**한다.

Grafana → Dashboards → New → **Import** → ID 입력 → 데이터소스 Prometheus 선택:

| ID | 대시보드 | 대상 지표 |
|----|----------|-----------|
| **4701** | JVM (Micrometer) | 힙/논힙, GC, 스레드 |
| **1860** | Node Exporter Full | 호스트 CPU/메모리/디스크 |
| **19908** | cAdvisor | 컨테이너별 CPU/메모리 |

> 이 3개를 provisioning JSON으로 넣지 않은 이유: 파일이 수천 줄이라 저장소가 비대해지고,
> 커뮤니티 대시보드는 버전 업데이트가 잦아 ID import 가 유지보수에 유리하기 때문. (ADR 002 참고)

## 확인 체크리스트

1. `http://<host>:9090/targets` → backend, node-exporter, cadvisor 가 **UP** 인지
2. `http://<host>:8080/actuator/prometheus` → 지표 텍스트가 나오는지
3. Grafana "NewsLens/Backend Overview" 대시보드에 그래프가 그려지는지
4. Grafana → Explore → Loki → `{service="backend"}` → 로그가 보이는지

## 주의

- promtail/cadvisor 는 도커 소켓·호스트 경로 마운트가 필요 → **Linux(EC2) 기준**. Windows 로컬에선 지표(Prometheus+Grafana)만 검증 가능.
- 전체 메모리 ~1GB. 소형 인스턴스면 loki/promtail 를 빼고 지표부터 운영.
