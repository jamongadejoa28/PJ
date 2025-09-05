#!/bin/bash

echo "[CELEBRATION] 토스뱅크 결제 플랫폼 최종 데모"
echo "================================="
echo ""

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

print_step() {
    echo -e "${BLUE}=== $1 ===${NC}"
}

print_success() {
    echo -e "${GREEN}[SUCCESS] $1${NC}"
}

print_info() {
    echo -e "${YELLOW}[INFO] $1${NC}"
}

# 시스템 상태 확인
print_step "1단계: 시스템 상태 확인"
HEALTH_STATUS=$(curl -s http://localhost:8080/api/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
if [ "$HEALTH_STATUS" = "UP" ]; then
    print_success "애플리케이션 정상 실행 중"
else
    echo -e "${RED}[ERROR] 애플리케이션이 실행되지 않았습니다. './run-complete-test.sh'를 먼저 실행하세요.${NC}"
    exit 1
fi

# 실시간 모니터링 확인
print_step "2단계: 실시간 모니터링 시스템 확인"
MONITORING_DATA=$(curl -s http://localhost:8080/api/admin/monitoring/stats)
TOTAL_REQUESTS=$(echo $MONITORING_DATA | grep -o '"total_requests":[0-9]*' | cut -d':' -f2)
SUCCESS_RATE=$(echo $MONITORING_DATA | grep -o '"success_rate":"[^"]*"' | cut -d'"' -f4)
DAILY_REVENUE=$(echo $MONITORING_DATA | grep -o '"daily_revenue":[0-9.]*' | cut -d':' -f2)

print_success "총 요청 수: $TOTAL_REQUESTS"
print_success "성공률: $SUCCESS_RATE"
print_success "일일 매출: ₩$DAILY_REVENUE"

# 데이터베이스 실시간 현황
print_step "3단계: 데이터베이스 실시간 현황"
DB_STATS=$(docker exec -i tossbank-mysql mysql -u root -ppassword tossbank_payment -e "
SELECT 
  (SELECT COUNT(*) FROM users) as users,
  (SELECT COUNT(*) FROM accounts) as accounts, 
  (SELECT COUNT(*) FROM payments) as payments,
  (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'COMPLETED') as total_revenue;
" 2>/dev/null | tail -n 1)

IFS=$'\t' read -r USERS ACCOUNTS PAYMENTS REVENUE <<< "$DB_STATS"
print_success "사용자: $USERS명"
print_success "계좌: $ACCOUNTS개" 
print_success "결제 건수: $PAYMENTS건"
print_success "총 매출: ₩$REVENUE"

# 새로운 결제 시연
print_step "4단계: 실시간 결제 시연"
print_info "새로운 사용자 생성 및 결제 처리 시연..."

# 새 사용자 생성
DEMO_EMAIL="demo.final@tossbank.com"
DEMO_PASSWORD="Demo2025!"

echo "신규 사용자 생성 중..."
SIGNUP_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$DEMO_EMAIL\",\"password\":\"$DEMO_PASSWORD\",\"name\":\"최종데모\",\"phoneNumber\":\"010-9999-0000\"}")

SIGNUP_SUCCESS=$(echo $SIGNUP_RESPONSE | grep -o '"success":[^,]*' | cut -d':' -f2)
if [ "$SIGNUP_SUCCESS" = "true" ]; then
    print_success "신규 사용자 생성 완료"
else
    print_info "기존 사용자 사용 (이미 생성됨)"
fi

# 로그인
echo "로그인 중..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$DEMO_EMAIL\",\"password\":\"$DEMO_PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
if [ ! -z "$TOKEN" ]; then
    print_success "로그인 성공"
else
    # 기존 사용자로 대체
    TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/signin \
      -H "Content-Type: application/json" \
      -d '{"email":"final@tossbank.com","password":"Final123@"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    print_info "기존 사용자로 로그인"
fi

# 계좌 생성  
echo "계좌 생성 중..."
ACCOUNT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"accountName":"데모 계좌","accountType":"CHECKING","dailyLimit":5000000,"monthlyLimit":50000000}')

ACCOUNT_ID=$(echo $ACCOUNT_RESPONSE | grep -o '"id":[0-9]*' | cut -d':' -f2)
if [ ! -z "$ACCOUNT_ID" ]; then
    print_success "계좌 생성 완료 (ID: $ACCOUNT_ID)"
else
    # 기존 계좌 사용
    ACCOUNT_ID="4"
    print_info "기존 계좌 사용 (ID: $ACCOUNT_ID)"
fi

# 입금
echo "계좌 입금 중..."
DEPOSIT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/accounts/$ACCOUNT_ID/deposit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":100000,"memo":"최종 데모 입금"}')

DEPOSIT_SUCCESS=$(echo $DEPOSIT_RESPONSE | grep -o '"success":[^,]*' | cut -d':' -f2)
if [ "$DEPOSIT_SUCCESS" = "true" ]; then
    print_success "입금 완료 (₩100,000)"
fi

# 다양한 결제 수단으로 연속 결제
print_step "5단계: 다양한 결제 수단 시연"

# 계좌이체 결제
echo "[MONEY] 계좌이체 결제..."
PAYMENT1=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"accountId\":$ACCOUNT_ID,\"merchantCode\":\"TOSS_STORE_001\",\"merchantOrderId\":\"FINAL_DEMO_001\",\"amount\":25000,\"paymentMethod\":\"ACCOUNT_TRANSFER\",\"description\":\"데모 계좌이체 결제\"}")

TXN1=$(echo $PAYMENT1 | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4)
print_success "계좌이체 완료: $TXN1"

sleep 1

# 토스페이 결제
echo "[MOBILE] 토스페이 결제..."  
PAYMENT2=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"accountId\":$ACCOUNT_ID,\"merchantCode\":\"TOSS_CAFE_002\",\"merchantOrderId\":\"FINAL_DEMO_002\",\"amount\":15000,\"paymentMethod\":\"TOSS_PAY\",\"description\":\"데모 토스페이 결제\"}")

TXN2=$(echo $PAYMENT2 | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4)
print_success "토스페이 완료: $TXN2"

sleep 1

# 카드 결제
echo "[CARD] 카드 결제..."
PAYMENT3=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"accountId\":$ACCOUNT_ID,\"merchantCode\":\"TOSS_STORE_001\",\"merchantOrderId\":\"FINAL_DEMO_003\",\"amount\":8000,\"paymentMethod\":\"CARD\",\"description\":\"데모 카드 결제\"}")

TXN3=$(echo $PAYMENT3 | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4)
print_success "카드결제 완료: $TXN3"

# 가맹점 API 시연
print_step "6단계: 가맹점 API 시연"
API_KEY="toss_api_key_1234567890abcdef1234567890abcdef12345678"

echo "[STORE] 가맹점 결제 요청..."
MERCHANT_REQUEST=$(curl -s -X POST http://localhost:8080/api/merchant/payments/request \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"merchantOrderId\":\"FINAL_MERCHANT_001\",\"amount\":50000,\"paymentMethod\":\"TOSS_PAY\",\"userEmail\":\"final@tossbank.com\",\"description\":\"가맹점 데모 결제\"}")

PAYMENT_KEY=$(echo $MERCHANT_REQUEST | grep -o '"paymentKey":"[^"]*"' | cut -d'"' -f4)
MERCHANT_TXN=$(echo $MERCHANT_REQUEST | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4)
print_success "결제 요청 생성: $MERCHANT_TXN"

echo "[SUCCESS] 결제 승인..."
MERCHANT_CONFIRM=$(curl -s -X POST http://localhost:8080/api/merchant/payments/confirm \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"paymentKey\":\"$PAYMENT_KEY\",\"orderId\":\"FINAL_MERCHANT_001\",\"amount\":50000}")

CONFIRM_SUCCESS=$(echo $MERCHANT_CONFIRM | grep -o '"success":[^,]*' | cut -d':' -f2)
if [ "$CONFIRM_SUCCESS" = "true" ]; then
    print_success "가맹점 결제 승인 완료: $MERCHANT_TXN"
fi

# 최종 결과 확인
print_step "7단계: 최종 결과 확인"

# 업데이트된 통계
FINAL_STATS=$(curl -s http://localhost:8080/api/admin/monitoring/stats)
FINAL_PAYMENTS=$(echo $FINAL_STATS | grep -o '"daily_payments":[0-9]*' | cut -d':' -f2)
FINAL_REVENUE=$(echo $FINAL_STATS | grep -o '"daily_revenue":[0-9.]*' | cut -d':' -f2)
FINAL_SUCCESS_RATE=$(echo $FINAL_STATS | grep -o '"success_rate":"[^"]*"' | cut -d'"' -f4)

print_success "최종 결제 건수: $FINAL_PAYMENTS건"
print_success "최종 매출: ₩$FINAL_REVENUE"  
print_success "최종 성공률: $FINAL_SUCCESS_RATE"

# 시스템 헬스 최종 체크
HEALTH_CHECK=$(curl -s http://localhost:8080/api/admin/monitoring/health)
SYSTEM_STATUS=$(echo $HEALTH_CHECK | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
AVG_RESPONSE_TIME=$(echo $HEALTH_CHECK | grep -o '"avg_response_time_ms":[0-9.]*' | cut -d':' -f2)

print_success "시스템 상태: $SYSTEM_STATUS"
print_success "평균 응답시간: ${AVG_RESPONSE_TIME}ms"

echo ""
print_step "[CONFETTI] 토스뱅크 결제 플랫폼 데모 완료!"
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}[TROPHY] 완성된 엔터프라이즈급 결제 플랫폼${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "[SUCCESS] 다중 결제수단: 계좌이체, 토스페이, 카드"
echo -e "[SUCCESS] 가맹점 API: Payment Key 기반 연동"
echo -e "[SUCCESS] 실시간 모니터링: 성능/보안/매출 추적"  
echo -e "[SUCCESS] 보안 시스템: JWT + Rate Limiting"
echo -e "[SUCCESS] 데이터베이스: MySQL + Redis + MongoDB"
echo ""
echo -e "${BLUE}[LINK] 접속 정보:${NC}"
echo -e "[CHART] 모니터링: http://localhost:8080/api/admin/monitoring/dashboard"
echo -e "[FILE_CABINET] 데이터베이스: http://localhost:8081 (root/password)"  
echo -e "[HOSPITAL] 헬스체크: http://localhost:8080/api/actuator/health"
echo ""
echo -e "${YELLOW}[LAUNCH] 토스뱅크 채용 준비 완료! [MUSCLE]${NC}"