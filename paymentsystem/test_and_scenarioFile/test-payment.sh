#!/bin/bash

echo "결제 시스템 테스트"
echo "=================="

# 1. 로그인
echo "1. 로그인 중..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"email":"final@tossbank.com","password":"Final123@"}')

echo "로그인 응답: $LOGIN_RESPONSE"

# JWT 토큰 추출
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "[ERROR] JWT 토큰을 가져올 수 없습니다"
    exit 1
fi

echo "[SUCCESS] JWT 토큰 획득: ${TOKEN:0:20}..."

# 2. 현재 잔액 확인
echo ""
echo "2. 현재 계좌 잔액 확인..."
BALANCE_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/accounts/4/balance" \
  -H "Authorization: Bearer $TOKEN")

echo "잔액 응답: $BALANCE_RESPONSE"

# 3. 결제 시도
echo ""
echo "3. 결제 시도..."
PAYMENT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 4,
    "merchantCode": "TOSS_STORE_001", 
    "merchantOrderId": "ORDER_TEST_001",
    "amount": 15000,
    "paymentMethod": "ACCOUNT_TRANSFER",
    "description": "테스트 결제"
  }')

echo "결제 응답: $PAYMENT_RESPONSE"

# 4. 결제 후 잔액 확인
echo ""  
echo "4. 결제 후 잔액 확인..."
BALANCE_AFTER_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/accounts/4/balance" \
  -H "Authorization: Bearer $TOKEN")

echo "결제 후 잔액: $BALANCE_AFTER_RESPONSE"

echo ""
echo "[SUCCESS] 테스트 완료!"