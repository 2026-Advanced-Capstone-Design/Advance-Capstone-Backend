#!/usr/bin/env bash
#
# Phase 9 정합성 결함 before/after 시연 스크립트
# ─────────────────────────────────────────────────────────────
# 사용법:
#   bash scripts/demo-phase9.sh          # 데모 A(멱등성) + B(스위퍼) 모두
#   bash scripts/demo-phase9.sh a        # 데모 A만 (9-1 멱등성)
#   bash scripts/demo-phase9.sh b        # 데모 B만 (9-4 스위퍼)
#
# before/after 토글:
#   git stash -u   → 수정 치워서 "before(버그)" 상태로 앱 재시작 후 실행
#   git stash pop  → 수정 복원해서 "after(고침)" 상태로 앱 재시작 후 실행
#   (권장: Phase 7~9를 커밋한 뒤 `git checkout HEAD~1` ↔ `git checkout -` 로 토글)
#
# 환경변수(기본값은 application.yaml 로컬 프로파일과 동일):
#   APP=http://localhost:8080
#   MYSQL="mysql -h127.0.0.1 -P3306 -uroot -p1234 factcheck"
#     · mysql CLI가 없으면 Docker로: MYSQL="docker exec -i <mysql컨테이너> mysql -uroot -p1234 factcheck"

set -u
APP="${APP:-http://localhost:8080}"
MYSQL="${MYSQL:-mysql -h127.0.0.1 -P3306 -uroot -p1234 factcheck}"

line() { printf '─%.0s' {1..64}; echo; }
title() { echo; line; echo "▶ $*"; line; }

# 앱 헬스체크
if ! curl -sf "$APP/health" >/dev/null 2>&1 && ! curl -sf "$APP/actuator/health" >/dev/null 2>&1; then
  echo "❌ 앱이 안 떠 있음: $APP  (먼저 Spring 앱을 실행하세요)"; exit 1
fi
echo "✅ 앱 응답 OK: $APP"

# MySQL에 INSERT 후 방금 생성된 id 반환 (같은 세션에서 LAST_INSERT_ID)
insert_article() {  # $1=status  $2=created_at_sql
  $MYSQL -N -s -e \
    "INSERT INTO articles (input_type, original_text, status, created_at)
     VALUES ('TEXT', 'phase9 demo body', '$1', $2);
     SELECT LAST_INSERT_ID();"
}

count_results() {  # $1=articleId
  $MYSQL -N -s -e "SELECT COUNT(*) FROM analysis_results WHERE ARTICLE_ID=$1;"
}

# ─────────────────────────────────────────────────────────────
# 데모 A — 9-1 콜백 멱등성 (같은 콜백 2번 → 결과 중복 → 조회 500)
# ─────────────────────────────────────────────────────────────
demo_a() {
  title "데모 A · 9-1 콜백 멱등성 (중복 콜백)"
  local ID; ID=$(insert_article "ANALYZING" "NOW()")
  echo "· 분석 대기 기사 생성: articleId=$ID (status=ANALYZING)"

  # 주의: JSON에 한글 금지 — Windows curl이 인자를 CP949로 변환해 Spring JSON 파싱 500 유발
  local CB="{\"article_id\":$ID,\"status\":\"DONE\",\"topic\":\"phase9-demo-topic\"}"

  echo; echo "STEP 1) 콜백 1회차 →"
  curl -s -o /dev/null -w "   HTTP %{http_code}\n" -X POST "$APP/api/v1/internal/callback" \
       -H 'Content-Type: application/json' -d "$CB"

  echo "STEP 2) 콜백 2회차 (중복) →"
  curl -s -o /dev/null -w "   HTTP %{http_code}\n" -X POST "$APP/api/v1/internal/callback" \
       -H 'Content-Type: application/json' -d "$CB"

  echo "STEP 3) 결과 조회 GET /{id}/result →"
  local CODE; CODE=$(curl -s -o /dev/null -w "%{http_code}" "$APP/api/v1/articles/$ID/result")
  echo "   HTTP $CODE"

  local N; N=$(count_results "$ID")
  echo "STEP 4) analysis_results 행 수 = $N"

  echo
  echo "📌 판정:"
  echo "   · after(고침): 2회차 콜백은 '중복 콜백 무시' 로그 → 결과행 1 → 조회 200"
  echo "   · before(버그): 결과행 2 → 조회 500(NonUniqueResultException)"
  echo "   → 이번 실행: 결과행=$N, 조회=HTTP $CODE"
}

# ─────────────────────────────────────────────────────────────
# 데모 B — 9-4 스위퍼 (콜백 유실 → stuck → 자동 FAILED 재조정)
# ─────────────────────────────────────────────────────────────
demo_b() {
  title "데모 B · 9-4 재조정 스위퍼 (콜백 유실 시뮬레이션)"
  echo "· 데모 전 준비: application.yaml 에 아래를 넣고 앱을 재시작했다고 가정"
  echo "    analysis: {sweeper: {stuck-minutes: 1, interval-ms: 10000}}"
  echo

  # 임계(1분)를 이미 넘긴 것처럼 created_at 을 5분 전으로
  local ID; ID=$(insert_article "ANALYZING" "DATE_SUB(NOW(), INTERVAL 5 MINUTE)")
  echo "· stuck 기사 생성: articleId=$ID (status=ANALYZING, 생성시각 5분 전, 콜백은 일부러 안 보냄)"
  echo

  echo "STEP) 상태를 최대 90초간 폴링 (스위퍼가 정리하는지 관찰) →"
  local i=0
  while [ $i -lt 18 ]; do
    local BODY; BODY=$(curl -s "$APP/api/v1/articles/$ID/status")
    printf "   [%s] %s\n" "$(date +%H:%M:%S)" "$BODY"
    if echo "$BODY" | grep -q '"FAILED"'; then
      echo; echo "✅ 스위퍼가 stuck 기사를 FAILED로 재조정함 (자가복구)"; return
    fi
    if echo "$BODY" | grep -q '"DONE"'; then
      echo; echo "ℹ️ DONE 됨 (예상 밖 — 콜백이 왔거나 데이터 상태 확인 필요)"; return
    fi
    sleep 5; i=$((i+1))
  done
  echo
  echo "📌 판정:"
  echo "   · after(고침): 임계시간 뒤 status가 ANALYZING → FAILED 로 자동 전환"
  echo "   · before(버그): 스위퍼 자체가 없어 계속 ANALYZING (영구 stuck)"
}

case "${1:-all}" in
  a) demo_a ;;
  b) demo_b ;;
  *) demo_a; demo_b ;;
esac
echo; echo "완료. 이 화면을 그대로 캡처하세요 (before/after 각각 1장씩)."
