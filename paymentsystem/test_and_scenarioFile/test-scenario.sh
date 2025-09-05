#!/bin/bash

echo "토스뱅크 결제 플랫폼 테스트 시작"
echo "=========================================="

# 서버 준비 대기
echo "...서버 연결 확인 중..."
for i in {1..30}; do
    if curl -s http://localhost:8080/api/actuator/health > /dev/null; then
        echo "[SUCCESS] 서버 연결 성공!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "[ERROR] 서버에 연결할 수 없습니다. 애플리케이션이 실행중인지 확인해주세요."
        exit 1
    fi
    sleep 1
done

# 1. Health Check
echo ""
echo "1. Health Check 테스트"
HEALTH_RESULT=$(curl -s http://localhost:8080/api/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
if [ "$HEALTH_RESULT" = "UP" ]; then
    echo "[SUCCESS] Health Check 성공: $HEALTH_RESULT"
else
    echo "[ERROR] Health Check 실패"
    exit 1
fi

# 2. 회원가입
echo ""
echo "2. 회원가입 테스트"
SIGNUP_RESULT=$(curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@tossbank.com",
    "password": "Demo123!",
    "name": "데모 사용자",
    "phoneNumber": "010-9999-8888"
  }')

SIGNUP_SUCCESS=$(echo "$SIGNUP_RESULT" | jq -r '.success' 2>/dev/null || echo "false")
if [ "$SIGNUP_SUCCESS" = "true" ]; then
    echo "[SUCCESS] 회원가입 성공"
else
    echo "[INFO] 회원가입 결과: $SIGNUP_RESULT"
fi

# 3. 로그인  
echo ""
echo "3. 로그인 테스트"
LOGIN_RESULT=$(curl -s -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@tossbank.com",
    "password": "Demo123!"
  }')

TOKEN=$(echo "$LOGIN_RESULT" | jq -r '.data.accessToken' 2>/dev/null || echo "null")
if [ "$TOKEN" != "null" ] && [ "$TOKEN" != "" ]; then
    echo "[SUCCESS] 로그인 성공 - JWT 토큰 획득"
else
    echo "[ERROR] 로그인 실패: $LOGIN_RESULT"
    exit 1
fi

# 4. 계좌 개설
echo ""
echo "4. 계좌 개설 테스트"
ACCOUNT_RESULT=$(curl -s -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountName": "데모 계좌",
    "accountType": "CHECKING",
    "dailyLimit": 5000000,
    "monthlyLimit": 50000000
  }')

ACCOUNT_ID=$(echo "$ACCOUNT_RESULT" | jq -r '.data.id' 2>/dev/null || echo "null")
ACCOUNT_NUMBER=$(echo "$ACCOUNT_RESULT" | jq -r '.data.accountNumber' 2>/dev/null || echo "null")

if [ "$ACCOUNT_ID" != "null" ] && [ "$ACCOUNT_ID" != "" ]; then
    echo "[SUCCESS] 계좌 개설 성공 - 계좌번호: $ACCOUNT_NUMBER"
else
    echo "[ERROR] 계좌 개설 실패: $ACCOUNT_RESULT"
    exit 1
fi

# 5. 입금
echo ""
echo "5. 입금 테스트"
DEPOSIT_RESULT=$(curl -s -X POST http://localhost:8080/api/accounts/$ACCOUNT_ID/deposit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500000,
    "memo": "초기 입금"
  }')

DEPOSIT_SUCCESS=$(echo "$DEPOSIT_RESULT" | jq -r '.success' 2>/dev/null || echo "false")
if [ "$DEPOSIT_SUCCESS" = "true" ]; then
    echo "[SUCCESS] 입금 성공 - 500,000원"
else
    echo "[ERROR] 입금 실패: $DEPOSIT_RESULT"
fi

# 6. 출금  
echo ""
echo "6. 출금 테스트"
WITHDRAW_RESULT=$(curl -s -X POST http://localhost:8080/api/accounts/$ACCOUNT_ID/withdraw \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100000,
    "memo": "테스트 출금"
  }')

WITHDRAW_SUCCESS=$(echo "$WITHDRAW_RESULT" | jq -r '.success' 2>/dev/null || echo "false")
if [ "$WITHDRAW_SUCCESS" = "true" ]; then
    echo "[SUCCESS] 출금 성공 - 100,000원"
else
    echo "[ERROR] 출금 실패: $WITHDRAW_RESULT"
fi

# 7. 최종 잔액 확인
echo ""
echo "7. 최종 잔액 확인"
BALANCE_RESULT=$(curl -s -X GET http://localhost:8080/api/accounts/$ACCOUNT_ID/balance \
  -H "Authorization: Bearer $TOKEN")

BALANCE=$(echo "$BALANCE_RESULT" | jq -r '.data.balance' 2>/dev/null || echo "null")
if [ "$BALANCE" != "null" ]; then
    echo "[SUCCESS] 최종 잔액: $BALANCE 원"
else
    echo "[ERROR] 잔액 조회 실패: $BALANCE_RESULT"
fi

echo ""
echo "=========================================="
echo "전체 테스트 시나리오 완료!"
echo "=========================================="