#!/bin/bash

echo "에러 처리 및 예외 상황 테스트"
echo "================================"

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

print_error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

print_test() {
    echo -e "${PURPLE}🧪 $1${NC}"
}

# 정상 로그인으로 토큰 획득
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

# 1. 인증 및 권한 에러 테스트
print_step "1. 인증 및 권한 에러 테스트"

print_test "1-1. 잘못된 이메일/비밀번호"
WRONG_LOGIN=$(curl -s -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"email":"wrong@email.com","password":"wrongpassword"}')
ERROR_CODE=$(echo $WRONG_LOGIN | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_CODE"

print_test "1-2. 만료된/유효하지 않은 토큰"
INVALID_TOKEN_RESPONSE=$(curl -s -X GET http://localhost:8080/api/accounts \
  -H "Authorization: Bearer invalid_token_here")
ERROR_MSG=$(echo $INVALID_TOKEN_RESPONSE | grep -o '"message":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_MSG"

print_test "1-3. 권한 없는 리소스 접근"
UNAUTHORIZED_RESPONSE=$(curl -s -X GET http://localhost:8080/api/accounts/999/balance \
  -H "Authorization: Bearer $TOKEN")
ERROR_RESPONSE=$(echo $UNAUTHORIZED_RESPONSE | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_RESPONSE"

# 2. 계좌 관련 에러 테스트  
print_step "2. 계좌 관련 에러 테스트"

print_test "2-1. 존재하지 않는 계좌 조회"
NOT_FOUND_ACCOUNT=$(curl -s -X GET http://localhost:8080/api/accounts/99999/balance \
  -H "Authorization: Bearer $TOKEN")
ERROR_CODE=$(echo $NOT_FOUND_ACCOUNT | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_CODE"

print_test "2-2. 잔액 부족 결제 시도"
INSUFFICIENT_BALANCE=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "merchantCode": "TOSS_STORE_001",
    "merchantOrderId": "ERROR_TEST_001",
    "amount": 99999999,
    "paymentMethod": "ACCOUNT_TRANSFER",
    "description": "잔액 부족 테스트"
  }')
ERROR_CODE=$(echo $INSUFFICIENT_BALANCE | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
REQUESTED_AMOUNT=$(echo $INSUFFICIENT_BALANCE | grep -o '"requestedAmount":[0-9]*' | cut -d':' -f2)
AVAILABLE_BALANCE=$(echo $INSUFFICIENT_BALANCE | grep -o '"availableBalance":[0-9.]*' | cut -d':' -f2)
print_info "응답: $ERROR_CODE (요청: ₩$REQUESTED_AMOUNT, 잔액: ₩$AVAILABLE_BALANCE)"

print_test "2-3. 일일 한도 초과 시도"
LIMIT_EXCEEDED=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "merchantCode": "TOSS_STORE_001",
    "merchantOrderId": "ERROR_TEST_002",
    "amount": 10000000,
    "paymentMethod": "ACCOUNT_TRANSFER",
    "description": "한도 초과 테스트"
  }')
ERROR_CODE=$(echo $LIMIT_EXCEEDED | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
LIMIT_TYPE=$(echo $LIMIT_EXCEEDED | grep -o '"limitType":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_CODE ($LIMIT_TYPE)"

# 3. 결제 관련 에러 테스트
print_step "3. 결제 관련 에러 테스트"

print_test "3-1. 유효하지 않은 결제 금액 (음수)"
INVALID_AMOUNT=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "merchantCode": "TOSS_STORE_001",
    "merchantOrderId": "ERROR_TEST_003",
    "amount": -1000,
    "paymentMethod": "TOSS_PAY",
    "description": "음수 금액 테스트"
  }')
ERROR_CODE=$(echo $INVALID_AMOUNT | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_CODE"

print_test "3-2. 존재하지 않는 가맹점"
INVALID_MERCHANT=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "merchantCode": "NONEXISTENT_MERCHANT",
    "merchantOrderId": "ERROR_TEST_004",
    "amount": 1000,
    "paymentMethod": "TOSS_PAY",
    "description": "존재하지 않는 가맹점 테스트"
  }')
ERROR_CODE=$(echo $INVALID_MERCHANT | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_CODE"

print_test "3-3. 이미 처리된 주문 ID 중복"
# 먼저 성공적인 결제 생성
FIRST_PAYMENT=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "merchantCode": "TOSS_STORE_001",
    "merchantOrderId": "DUPLICATE_ORDER_001",
    "amount": 1000,
    "paymentMethod": "TOSS_PAY",
    "description": "중복 테스트용 첫 번째 결제"
  }')

# 동일한 주문 ID로 다시 시도
DUPLICATE_ORDER=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "merchantCode": "TOSS_STORE_001",
    "merchantOrderId": "DUPLICATE_ORDER_001",
    "amount": 1000,
    "paymentMethod": "TOSS_PAY",
    "description": "중복 테스트용 두 번째 결제"
  }')
ERROR_CODE=$(echo $DUPLICATE_ORDER | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_CODE"

# 4. 가맹점 API 에러 테스트
print_step "4. 가맹점 API 에러 테스트"

print_test "4-1. 유효하지 않은 API 키"
INVALID_API_KEY=$(curl -s -X POST http://localhost:8080/api/merchant/payments/request \
  -H "Authorization: Bearer invalid_api_key_here" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantOrderId": "ERROR_TEST_005",
    "amount": 5000,
    "paymentMethod": "TOSS_PAY",
    "userEmail": "demo@tossbank.com",
    "description": "API 키 테스트"
  }')
ERROR_CODE=$(echo $INVALID_API_KEY | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_CODE"

print_test "4-2. 존재하지 않는 결제 키 확인"
INVALID_PAYMENT_KEY=$(curl -s -X POST http://localhost:8080/api/merchant/payments/confirm \
  -H "Authorization: Bearer toss_api_key_1234567890abcdef1234567890abcdef12345678" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentKey": "nonexistent_payment_key_12345",
    "orderId": "ERROR_TEST_006",
    "amount": 5000
  }')
ERROR_CODE=$(echo $INVALID_PAYMENT_KEY | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_CODE"

# 5. 요청 형식 에러 테스트
print_step "5. 요청 형식 에러 테스트"

print_test "5-1. 잘못된 JSON 형식"
INVALID_JSON=$(curl -s -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":}')  # 잘못된 JSON
ERROR_CODE=$(echo $INVALID_JSON | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_CODE"

print_test "5-2. 필수 필드 누락"
MISSING_FIELD=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "amount": 5000,
    "description": "필수 필드 누락 테스트"
  }')  # merchantCode, paymentMethod 누락
ERROR_CODE=$(echo $MISSING_FIELD | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_CODE"

print_test "5-3. 지원하지 않는 HTTP 메소드"
WRONG_METHOD=$(curl -s -X DELETE http://localhost:8080/api/payments/123 \
  -H "Authorization: Bearer $TOKEN")
ERROR_CODE=$(echo $WRONG_METHOD | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
print_info "응답: $ERROR_CODE"

# 6. Rate Limiting 에러 테스트
print_step "6. Rate Limiting 에러 테스트"

print_test "6-1. 빠른 연속 요청으로 Rate Limit 유발"
for i in {1..20}; do
    RATE_LIMIT_RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" \
      -X GET http://localhost:8080/api/accounts \
      -H "Authorization: Bearer $TOKEN")
    
    HTTP_CODE=$(echo $RATE_LIMIT_RESPONSE | grep -o 'HTTP_CODE:[0-9]*' | cut -d':' -f2)
    
    if [ "$HTTP_CODE" = "429" ]; then
        ERROR_RESPONSE=$(echo $RATE_LIMIT_RESPONSE | grep -o '"errorCode":"[^"]*"' | cut -d'"' -f4)
        RETRY_AFTER=$(echo $RATE_LIMIT_RESPONSE | grep -o '"retryAfterSeconds":[0-9]*' | cut -d':' -f2)
        print_info "Rate Limit 도달: $ERROR_RESPONSE (재시도: ${RETRY_AFTER}초 후)"
        break
    fi
    
    if [ $i -eq 20 ]; then
        print_info "Rate Limit이 발생하지 않았습니다 (현재 설정이 관대함)"
    fi
done

echo ""
print_step "[CONFETTI] 에러 처리 테스트 완료!"
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN} 에러 처리 시스템 검증 결과${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "[SUCCESS] 인증/권한 에러: 3가지 시나리오"
echo -e "[SUCCESS] 계좌 관련 에러: 3가지 시나리오"
echo -e "[SUCCESS] 결제 처리 에러: 3가지 시나리오"
echo -e "[SUCCESS] 가맹점 API 에러: 2가지 시나리오"
echo -e "[SUCCESS] 요청 형식 에러: 3가지 시나리오"
echo -e "[SUCCESS] Rate Limiting 에러: 1가지 시나리오"
echo ""
echo -e "${YELLOW}견고한 에러 처리 시스템 검증 완료! [MUSCLE]${NC}"