#!/bin/bash

echo "[STORE] 가맹점 API 테스트"
echo "=================="

# 가맹점 API 키 (DataLoader에서 생성한 것)
API_KEY="toss_api_key_1234567890abcdef1234567890abcdef12345678"

echo "🔑 가맹점 API 키: ${API_KEY:0:20}..."

# 1. 결제 요청 생성
echo ""
echo "1. 가맹점 결제 요청 생성..."
MERCHANT_PAYMENT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/merchant/payments/request \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantOrderId": "MERCHANT_ORDER_001",
    "amount": 50000,
    "paymentMethod": "TOSS_PAY",
    "userEmail": "final@tossbank.com",
    "description": "가맹점 API 테스트 결제",
    "successUrl": "https://merchant.example.com/success",
    "failUrl": "https://merchant.example.com/fail"
  }')

echo "가맹점 결제 요청 응답: $MERCHANT_PAYMENT_RESPONSE"

# Payment Key 추출
PAYMENT_KEY=$(echo $MERCHANT_PAYMENT_RESPONSE | grep -o '"paymentKey":"[^"]*"' | cut -d'"' -f4)
TRANSACTION_ID=$(echo $MERCHANT_PAYMENT_RESPONSE | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$PAYMENT_KEY" ]; then
    echo "[ERROR] Payment Key를 가져올 수 없습니다"
    exit 1
fi

echo "[SUCCESS] Payment Key 획득: ${PAYMENT_KEY:0:20}..."
echo "[SUCCESS] Transaction ID: $TRANSACTION_ID"

# 2. 결제 승인 (confirm)
echo ""
echo "2. 결제 승인 처리..."
CONFIRM_RESPONSE=$(curl -s -X POST http://localhost:8080/api/merchant/payments/confirm \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"paymentKey\": \"$PAYMENT_KEY\",
    \"orderId\": \"MERCHANT_ORDER_001\",
    \"amount\": 50000
  }")

echo "결제 승인 응답: $CONFIRM_RESPONSE"

# 3. 결제 상태 조회
echo ""
echo "3. 가맹점에서 결제 상태 조회..."
STATUS_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/merchant/payments/$TRANSACTION_ID" \
  -H "Authorization: Bearer $API_KEY")

echo "결제 상태 응답: $STATUS_RESPONSE"

# 4. 잘못된 API 키로 테스트
echo ""
echo "4. 잘못된 API 키로 접근 테스트..."
INVALID_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/merchant/payments/$TRANSACTION_ID" \
  -H "Authorization: Bearer invalid_api_key_123")

echo "잘못된 API 키 응답: $INVALID_RESPONSE"

echo ""
echo "[SUCCESS] 가맹점 API 테스트 완료!"