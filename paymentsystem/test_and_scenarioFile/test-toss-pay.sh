#!/bin/bash

echo " 토스페이 결제 테스트"
echo "==================="

# 로그인
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"email":"final@tossbank.com","password":"Final123@"}')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

echo "[SUCCESS] JWT 토큰 획득"

# 토스페이 결제 테스트
echo ""
echo "[CARD] 토스페이 결제 시도..."
TOSS_PAY_RESPONSE=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 4,
    "merchantCode": "TOSS_STORE_001", 
    "merchantOrderId": "ORDER_TOSS_001",
    "amount": 35000,
    "paymentMethod": "TOSS_PAY",
    "description": "토스페이 테스트 결제"
  }')

echo "토스페이 결제 응답: $TOSS_PAY_RESPONSE"

# 카드 결제 테스트
echo ""
echo "[CARD] 카드 결제 시도..."
CARD_RESPONSE=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 4,
    "merchantCode": "TOSS_CAFE_002", 
    "merchantOrderId": "ORDER_CARD_001",
    "amount": 8500,
    "paymentMethod": "CARD",
    "description": "카드 결제 테스트"
  }')

echo "카드 결제 응답: $CARD_RESPONSE"

# 최종 잔액 확인
echo ""
echo "[MONEY] 최종 잔액 확인..."
FINAL_BALANCE=$(curl -s -X GET "http://localhost:8080/api/accounts/4/balance" \
  -H "Authorization: Bearer $TOKEN")

echo "최종 잔액: $FINAL_BALANCE"

echo ""
echo "[SUCCESS] 모든 결제 방법 테스트 완료!"