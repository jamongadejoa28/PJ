#!/bin/bash

echo "[ZAP] 결제 API 성능 비교 테스트"
echo "============================"

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

# 로그인 및 토큰 획득
print_step "인증 토큰 획득"
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@tossbank.com","password":"Demo123!"}')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
if [ ! -z "$TOKEN" ]; then
    print_success "JWT 토큰 획득 완료"
else
    print_error "토큰 획득 실패"
    exit 1
fi

# 테스트 설정
ITERATIONS=20
PAYMENT_AMOUNT_BASE=1000

# 기존 API 성능 테스트
print_step "기존 API 성능 테스트 (${ITERATIONS}회)"

ORIGINAL_TIMES=()
ORIGINAL_SUCCESS=0

for i in $(seq 1 $ITERATIONS); do
    AMOUNT=$((PAYMENT_AMOUNT_BASE + i * 100))
    
    START_TIME=$(date +%s%N)
    
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/payments \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"accountId\": 1,
        \"merchantCode\": \"TOSS_STORE_001\",
        \"merchantOrderId\": \"PERF_ORIG_$(printf '%03d' $i)\",
        \"amount\": $AMOUNT,
        \"paymentMethod\": \"TOSS_PAY\",
        \"description\": \"기존 API 성능 테스트 $i\"
      }")
    
    END_TIME=$(date +%s%N)
    RESPONSE_TIME=$(( ($END_TIME - $START_TIME) / 1000000 ))
    
    SUCCESS=$(echo $RESPONSE | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    ORIGINAL_TIMES+=($RESPONSE_TIME)
    
    if [ "$SUCCESS" = "true" ]; then
        ((ORIGINAL_SUCCESS++))
        print_info "  테스트 $i: ${RESPONSE_TIME}ms [SUCCESS]"
    else
        print_info "  테스트 $i: ${RESPONSE_TIME}ms [ERROR]"
    fi
    
    sleep 0.2
done

# 최적화된 API 성능 테스트  
print_step "최적화된 API 성능 테스트 (${ITERATIONS}회)"

OPTIMIZED_TIMES=()
OPTIMIZED_SUCCESS=0

for i in $(seq 1 $ITERATIONS); do
    AMOUNT=$((PAYMENT_AMOUNT_BASE + i * 100 + 50000)) # 금액을 다르게 해서 구분
    
    START_TIME=$(date +%s%N)
    
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/payments/optimized \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"accountId\": 1,
        \"merchantCode\": \"TOSS_STORE_001\",
        \"merchantOrderId\": \"PERF_OPT_$(printf '%03d' $i)\",
        \"amount\": $AMOUNT,
        \"paymentMethod\": \"TOSS_PAY\",
        \"description\": \"최적화 API 성능 테스트 $i\"
      }")
    
    END_TIME=$(date +%s%N)
    RESPONSE_TIME=$(( ($END_TIME - $START_TIME) / 1000000 ))
    
    SUCCESS=$(echo $RESPONSE | grep -o '"success":[^,]*' | cut -d':' -f2)
    
    OPTIMIZED_TIMES+=($RESPONSE_TIME)
    
    if [ "$SUCCESS" = "true" ]; then
        ((OPTIMIZED_SUCCESS++))
        print_info "  테스트 $i: ${RESPONSE_TIME}ms [SUCCESS]"
    else
        print_info "  테스트 $i: ${RESPONSE_TIME}ms [ERROR]"
    fi
    
    sleep 0.2
done

# 통계 계산
print_step "성능 비교 결과"

# 평균 계산 함수
calculate_average() {
    local times=("$@")
    local sum=0
    local count=${#times[@]}
    
    for time in "${times[@]}"; do
        sum=$((sum + time))
    done
    
    echo $((sum / count))
}

# 최소값 계산 함수  
calculate_min() {
    local times=("$@")
    local min=${times[0]}
    
    for time in "${times[@]}"; do
        if [ $time -lt $min ]; then
            min=$time
        fi
    done
    
    echo $min
}

# 최대값 계산 함수
calculate_max() {
    local times=("$@")
    local max=${times[0]}
    
    for time in "${times[@]}"; do
        if [ $time -gt $max ]; then
            max=$time
        fi
    done
    
    echo $max
}

ORIGINAL_AVG=$(calculate_average "${ORIGINAL_TIMES[@]}")
ORIGINAL_MIN=$(calculate_min "${ORIGINAL_TIMES[@]}")
ORIGINAL_MAX=$(calculate_max "${ORIGINAL_TIMES[@]}")

OPTIMIZED_AVG=$(calculate_average "${OPTIMIZED_TIMES[@]}")
OPTIMIZED_MIN=$(calculate_min "${OPTIMIZED_TIMES[@]}")
OPTIMIZED_MAX=$(calculate_max "${OPTIMIZED_TIMES[@]}")

print_result "기존 API 결과:"
print_result "  평균 응답시간: ${ORIGINAL_AVG}ms"
print_result "  최소 응답시간: ${ORIGINAL_MIN}ms"
print_result "  최대 응답시간: ${ORIGINAL_MAX}ms"
print_result "  성공률: $(( ORIGINAL_SUCCESS * 100 / ITERATIONS ))% (${ORIGINAL_SUCCESS}/${ITERATIONS})"

print_result "최적화 API 결과:"
print_result "  평균 응답시간: ${OPTIMIZED_AVG}ms"
print_result "  최소 응답시간: ${OPTIMIZED_MIN}ms"  
print_result "  최대 응답시간: ${OPTIMIZED_MAX}ms"
print_result "  성공률: $(( OPTIMIZED_SUCCESS * 100 / ITERATIONS ))% (${OPTIMIZED_SUCCESS}/${ITERATIONS})"

# 개선율 계산
IMPROVEMENT=$(( (ORIGINAL_AVG - OPTIMIZED_AVG) * 100 / ORIGINAL_AVG ))
SUCCESS_IMPROVEMENT=$(( (OPTIMIZED_SUCCESS - ORIGINAL_SUCCESS) * 100 / ORIGINAL_SUCCESS ))

echo ""
print_step " 성능 개선 결과"
if [ $OPTIMIZED_AVG -lt $ORIGINAL_AVG ]; then
    print_success "응답시간 ${IMPROVEMENT}% 개선! [CELEBRATION]"
else
    DEGRADATION=$(( (OPTIMIZED_AVG - ORIGINAL_AVG) * 100 / ORIGINAL_AVG ))
    print_info "응답시간 ${DEGRADATION}% 증가 (추가 분석 필요)"
fi

if [ $OPTIMIZED_SUCCESS -gt $ORIGINAL_SUCCESS ]; then
    print_success "성공률 ${SUCCESS_IMPROVEMENT}% 개선!"
elif [ $OPTIMIZED_SUCCESS -eq $ORIGINAL_SUCCESS ]; then
    print_info "성공률 동일 유지"
else
    print_info "성공률 개선 필요"
fi

echo ""
print_step " 목표 달성 여부"
TARGET_RESPONSE_TIME=150
TARGET_SUCCESS_RATE=95

if [ $OPTIMIZED_AVG -le $TARGET_RESPONSE_TIME ]; then
    print_success "목표 응답시간 달성! (${OPTIMIZED_AVG}ms ≤ ${TARGET_RESPONSE_TIME}ms)"
else
    print_info "목표 응답시간 미달성 (${OPTIMIZED_AVG}ms > ${TARGET_RESPONSE_TIME}ms)"
fi

OPTIMIZED_SUCCESS_RATE=$(( OPTIMIZED_SUCCESS * 100 / ITERATIONS ))
if [ $OPTIMIZED_SUCCESS_RATE -ge $TARGET_SUCCESS_RATE ]; then
    print_success "목표 성공률 달성! (${OPTIMIZED_SUCCESS_RATE}% ≥ ${TARGET_SUCCESS_RATE}%)"
else
    print_info "목표 성공률 미달성 (${OPTIMIZED_SUCCESS_RATE}% < ${TARGET_SUCCESS_RATE}%)"
fi

echo ""
print_step "[FLOPPY_DISK] 결과 저장"
cat > performance_test_results.json << EOF
{
  "test_date": "$(date -Iseconds)",
  "iterations": $ITERATIONS,
  "original_api": {
    "avg_response_time_ms": $ORIGINAL_AVG,
    "min_response_time_ms": $ORIGINAL_MIN,
    "max_response_time_ms": $ORIGINAL_MAX,
    "success_rate": $(( ORIGINAL_SUCCESS * 100 / ITERATIONS )),
    "success_count": $ORIGINAL_SUCCESS
  },
  "optimized_api": {
    "avg_response_time_ms": $OPTIMIZED_AVG,
    "min_response_time_ms": $OPTIMIZED_MIN,
    "max_response_time_ms": $OPTIMIZED_MAX,
    "success_rate": $(( OPTIMIZED_SUCCESS * 100 / ITERATIONS )),
    "success_count": $OPTIMIZED_SUCCESS
  },
  "improvement": {
    "response_time_improvement_percent": $IMPROVEMENT,
    "meets_target_response_time": $([ $OPTIMIZED_AVG -le $TARGET_RESPONSE_TIME ] && echo "true" || echo "false"),
    "meets_target_success_rate": $([ $OPTIMIZED_SUCCESS_RATE -ge $TARGET_SUCCESS_RATE ] && echo "true" || echo "false")
  }
}
EOF

print_success "테스트 결과가 performance_test_results.json에 저장되었습니다"

echo ""
print_step "[CONFETTI] 성능 비교 테스트 완료!"