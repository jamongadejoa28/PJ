#!/bin/bash

echo " 토스뱅크 API 빠른 테스트"
echo "=========================="

# 1. Health Check
echo "1. Health Check:"
curl -s http://localhost:8080/api/actuator/health
echo ""
echo ""

# 2. 회원가입 테스트
echo "2. 회원가입 테스트:"
SIGNUP_RESULT=$(curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"quicktest@tossbank.com","password":"Quick123@","name":"퀵테스트","phoneNumber":"010-7777-8888"}')
echo $SIGNUP_RESULT
echo ""

# 3. 로그인 테스트
echo "3. 로그인 테스트:"
LOGIN_RESULT=$(curl -s -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"email":"quicktest@tossbank.com","password":"Quick123@"}')
echo $LOGIN_RESULT
echo ""

# JWT 토큰 추출
TOKEN=$(echo $LOGIN_RESULT | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ ! -z "$TOKEN" ]; then
    echo "4. 인증된 사용자 정보 조회:"
    curl -s -X GET http://localhost:8080/api/auth/me \
      -H "Authorization: Bearer $TOKEN"
    echo ""
    echo ""

    echo "5. 계좌 목록 조회:"
    curl -s -X GET http://localhost:8080/api/accounts \
      -H "Authorization: Bearer $TOKEN"
    echo ""
    echo ""

    echo "6. 새 계좌 생성:"
    ACCOUNT_RESULT=$(curl -s -X POST http://localhost:8080/api/accounts \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"accountName":"퀵테스트 계좌","accountType":"CHECKING","dailyLimit":5000000,"monthlyLimit":50000000}')
    echo $ACCOUNT_RESULT
    echo ""
    echo ""

    # 계좌 ID 추출
    ACCOUNT_ID=$(echo $ACCOUNT_RESULT | grep -o '"id":[0-9]*' | cut -d':' -f2)

    if [ ! -z "$ACCOUNT_ID" ]; then
        echo "7. 입금 테스트 (30만원):"
        curl -s -X POST http://localhost:8080/api/accounts/$ACCOUNT_ID/deposit \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d '{"amount":300000,"memo":"퀵테스트 입금"}'
        echo ""
        echo ""

        echo "8. 잔액 조회:"
        curl -s -X GET http://localhost:8080/api/accounts/$ACCOUNT_ID/balance \
          -H "Authorization: Bearer $TOKEN"
        echo ""
    fi
fi

echo ""
echo "[SUCCESS] 테스트 완료!"
echo "[CHART] 데이터베이스 확인: http://localhost:8081"
echo "[HOSPITAL] Health Check: http://localhost:8080/api/actuator/health"