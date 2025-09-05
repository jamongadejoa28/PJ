#!/bin/bash

echo "[LAUNCH] 대용량 트래픽 부하 테스트"
echo "============================="

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
PURPLE='\033[0;35m'
NC='\033[0m'

print_step() {
    echo -e "${BLUE}=== $1 ===${NC}"
}

print_success() {
    echo -e "${GREEN}[SUCCESS] $1${NC}"
}

print_info() {
    echo -e "${YELLOW}[INFO] $1${NC}"
}

print_result() {
    echo -e "${PURPLE}[CHART] $1${NC}"
}

print_error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

# 설정
TARGET_TPS=${1:-100}  # 기본값 100 TPS, 인자로 변경 가능
DURATION=${2:-10}     # 기본값 10초
CONCURRENT_USERS=${3:-20}  # 기본값 20명 동시 사용자

print_step "부하 테스트 설정"
print_info "목표 TPS: $TARGET_TPS"
print_info "테스트 시간: ${DURATION}초"
print_info "동시 사용자: $CONCURRENT_USERS명"

# 토큰 획득
print_step "인증 토큰 획득"
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"email":"final@tossbank.com","password":"Final123@"}')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then
    print_error "토큰 획득 실패"
    exit 1
fi
print_success "JWT 토큰 획득 완료"

# 결과 파일 초기화
RESULT_FILE="load_test_results_$(date +%Y%m%d_%H%M%S).csv"
echo "timestamp,response_time_ms,success,tps,concurrent_requests" > $RESULT_FILE

# 동시성 테스트를 위한 백그라운드 함수
run_load_test_worker() {
    local worker_id=$1
    local requests_per_worker=$((TARGET_TPS * DURATION / CONCURRENT_USERS))
    local delay_ms=$((1000 / (TARGET_TPS / CONCURRENT_USERS)))
    
    for i in $(seq 1 $requests_per_worker); do
        local start_time=$(date +%s%N)
        local timestamp=$(date -Iseconds)
        
        # 동적 결제 요청 생성
        local order_id="LOAD_W${worker_id}_${i}"
        local amount=$((1000 + worker_id * 100 + i * 10))
        
        local response=$(curl -s -X POST http://localhost:8080/api/payments \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d "{
            \"accountId\": 4,
            \"merchantCode\": \"TOSS_STORE_001\",
            \"merchantOrderId\": \"$order_id\",
            \"amount\": $amount,
            \"paymentMethod\": \"TOSS_PAY\",
            \"description\": \"부하테스트 Worker$worker_id Request$i\"
          }")
        
        local end_time=$(date +%s%N)
        local response_time=$(( (end_time - start_time) / 1000000 ))
        
        local success=$(echo $response | grep -o '"success":[^,]*' | cut -d':' -f2)
        local success_flag=$([ "$success" = "true" ] && echo "1" || echo "0")
        
        # 결과 기록
        echo "$timestamp,$response_time,$success_flag,0,0" >> "${RESULT_FILE}.worker$worker_id"
        
        # TPS 조절을 위한 지연
        if [ $delay_ms -gt 0 ]; then
            sleep $(echo "scale=3; $delay_ms/1000" | bc -l) 2>/dev/null || sleep 0.05
        fi
    done
}

# 부하 테스트 실행
print_step "부하 테스트 시작"
print_info "$(date): 테스트 시작..."

# 백그라운드로 워커들 실행
for worker_id in $(seq 1 $CONCURRENT_USERS); do
    run_load_test_worker $worker_id &
    print_info "Worker $worker_id 시작됨 (PID: $!)"
done

# 실시간 모니터링
print_step "실시간 모니터링"
for second in $(seq 1 $DURATION); do
    sleep 1
    
    # 현재 시스템 상태 확인
    STATS_RESPONSE=$(curl -s http://localhost:8080/api/admin/monitoring/stats 2>/dev/null || echo '{"total_requests":0}')
    CURRENT_REQUESTS=$(echo $STATS_RESPONSE | grep -o '"total_requests":[0-9]*' | cut -d':' -f2 || echo "0")
    
    print_info "진행 시간: ${second}/${DURATION}초, 총 요청: $CURRENT_REQUESTS건"
done

# 모든 워커 완료 대기
print_step "워커 완료 대기"
wait

# 결과 집계
print_step "결과 집계 중"

# 모든 워커 결과 병합
for worker_id in $(seq 1 $CONCURRENT_USERS); do
    if [ -f "${RESULT_FILE}.worker$worker_id" ]; then
        tail -n +2 "${RESULT_FILE}.worker$worker_id" >> $RESULT_FILE
        rm "${RESULT_FILE}.worker$worker_id"
    fi
done

# 통계 계산
if [ -f "$RESULT_FILE" ]; then
    TOTAL_REQUESTS=$(tail -n +2 $RESULT_FILE | wc -l)
    SUCCESS_REQUESTS=$(tail -n +2 $RESULT_FILE | awk -F',' '$3==1' | wc -l)
    FAILED_REQUESTS=$((TOTAL_REQUESTS - SUCCESS_REQUESTS))
    
    if [ $TOTAL_REQUESTS -gt 0 ]; then
        AVG_RESPONSE_TIME=$(tail -n +2 $RESULT_FILE | awk -F',' '{sum+=$2} END {printf "%.1f", sum/NR}')
        SUCCESS_RATE=$(( SUCCESS_REQUESTS * 100 / TOTAL_REQUESTS ))
        ACTUAL_TPS=$(( TOTAL_REQUESTS / DURATION ))
        
        # 최소/최대 응답시간
        MIN_RESPONSE=$(tail -n +2 $RESULT_FILE | awk -F',' '{print $2}' | sort -n | head -1)
        MAX_RESPONSE=$(tail -n +2 $RESULT_FILE | awk -F',' '{print $2}' | sort -n | tail -1)
    else
        AVG_RESPONSE_TIME=0
        SUCCESS_RATE=0
        ACTUAL_TPS=0
        MIN_RESPONSE=0
        MAX_RESPONSE=0
    fi
else
    TOTAL_REQUESTS=0
    SUCCESS_REQUESTS=0
    FAILED_REQUESTS=0
    AVG_RESPONSE_TIME=0
    SUCCESS_RATE=0
    ACTUAL_TPS=0
    MIN_RESPONSE=0
    MAX_RESPONSE=0
fi

# 최종 시스템 상태
FINAL_STATS=$(curl -s http://localhost:8080/api/admin/monitoring/stats 2>/dev/null || echo '{"total_requests":0,"daily_revenue":0}')
FINAL_TOTAL_REQUESTS=$(echo $FINAL_STATS | grep -o '"total_requests":[0-9]*' | cut -d':' -f2 || echo "0")
FINAL_REVENUE=$(echo $FINAL_STATS | grep -o '"daily_revenue":[0-9.]*' | cut -d':' -f2 || echo "0")

# 결과 출력
echo ""
print_step " 부하 테스트 결과"
print_result "테스트 설정:"
print_result "  목표 TPS: $TARGET_TPS"
print_result "  테스트 시간: ${DURATION}초"
print_result "  동시 사용자: $CONCURRENT_USERS명"

echo ""
print_result "성능 지표:"
print_result "  총 요청 수: $TOTAL_REQUESTS건"
print_result "  성공 요청: $SUCCESS_REQUESTS건"
print_result "  실패 요청: $FAILED_REQUESTS건"
print_result "  성공률: ${SUCCESS_RATE}%"
print_result "  실제 TPS: ${ACTUAL_TPS}"

echo ""
print_result "응답시간 분석:"
print_result "  평균 응답시간: ${AVG_RESPONSE_TIME}ms"
print_result "  최소 응답시간: ${MIN_RESPONSE}ms"
print_result "  최대 응답시간: ${MAX_RESPONSE}ms"

echo ""
print_result "시스템 상태:"
print_result "  전체 누적 요청: $FINAL_TOTAL_REQUESTS건"
print_result "  일일 매출: ₩$FINAL_REVENUE"

# 목표 달성 여부
echo ""
print_step "[TROPHY] 목표 달성 평가"

TPS_ACHIEVEMENT=$(( ACTUAL_TPS * 100 / TARGET_TPS ))
if [ $ACTUAL_TPS -ge $TARGET_TPS ]; then
    print_success "TPS 목표 달성! ($ACTUAL_TPS >= $TARGET_TPS)"
else
    print_info "TPS 목표 미달성 ($ACTUAL_TPS < $TARGET_TPS) - 달성률: ${TPS_ACHIEVEMENT}%"
fi

if [ $SUCCESS_RATE -ge 95 ]; then
    print_success "성공률 목표 달성! (${SUCCESS_RATE}% >= 95%)"
else
    print_info "성공률 개선 필요 (${SUCCESS_RATE}% < 95%)"
fi

if [ $(echo "$AVG_RESPONSE_TIME <= 200" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
    print_success "응답시간 목표 달성! (${AVG_RESPONSE_TIME}ms <= 200ms)"
else
    print_info "응답시간 개선 필요 (${AVG_RESPONSE_TIME}ms > 200ms)"
fi

# JSON 결과 저장
RESULT_JSON="load_test_summary_$(date +%Y%m%d_%H%M%S).json"
cat > $RESULT_JSON << EOF
{
  "test_config": {
    "target_tps": $TARGET_TPS,
    "duration_seconds": $DURATION,
    "concurrent_users": $CONCURRENT_USERS,
    "start_time": "$(date -Iseconds)"
  },
  "results": {
    "total_requests": $TOTAL_REQUESTS,
    "success_requests": $SUCCESS_REQUESTS,
    "failed_requests": $FAILED_REQUESTS,
    "success_rate_percent": $SUCCESS_RATE,
    "actual_tps": $ACTUAL_TPS,
    "avg_response_time_ms": $AVG_RESPONSE_TIME,
    "min_response_time_ms": $MIN_RESPONSE,
    "max_response_time_ms": $MAX_RESPONSE
  },
  "system_state": {
    "total_system_requests": $FINAL_TOTAL_REQUESTS,
    "daily_revenue": $FINAL_REVENUE
  },
  "achievements": {
    "tps_target_met": $([ $ACTUAL_TPS -ge $TARGET_TPS ] && echo "true" || echo "false"),
    "success_rate_target_met": $([ $SUCCESS_RATE -ge 95 ] && echo "true" || echo "false"),
    "response_time_target_met": $([ $(echo "$AVG_RESPONSE_TIME <= 200" | bc -l 2>/dev/null || echo "0") -eq 1 ] && echo "true" || echo "false")
  }
}
EOF

print_success "상세 결과가 저장되었습니다:"
print_success "  CSV 데이터: $RESULT_FILE"
print_success "  JSON 요약: $RESULT_JSON"

echo ""
print_step "[CELEBRATION] 대용량 트래픽 부하 테스트 완료!"
echo ""
echo -e "${BLUE}사용법:${NC}"
echo -e "  ./load-test.sh [TPS] [시간] [동시사용자]"
echo -e "  예: ./load-test.sh 500 30 50  # 500 TPS, 30초, 50명 동시사용자"