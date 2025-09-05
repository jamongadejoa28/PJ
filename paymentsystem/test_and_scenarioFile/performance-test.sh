#!/bin/bash

echo "[LAUNCH] 토스뱅크 성능 및 보안 테스트"
echo "================================"

# 로그인하여 JWT 토큰 획득
echo "1. JWT 토큰 획득..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"email":"final@tossbank.com","password":"Final123@"}')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
echo "[SUCCESS] JWT 토큰 획득 완료"

# 현재 모니터링 상태 확인
echo ""
echo "2. 시스템 상태 확인..."
curl -s http://localhost:8080/api/admin/monitoring/dashboard | head -200
echo ""

# 성능 테스트: 연속 결제 요청 (5회)
echo ""
echo "3. 성능 테스트 - 연속 결제 5회..."
for i in {1..5}; do
    echo "결제 $i 진행 중..."
    
    START_TIME=$(date +%s%N)
    
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/payments \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"accountId\": 4,
        \"merchantCode\": \"TOSS_STORE_001\",
        \"merchantOrderId\": \"PERF_TEST_$i\",
        \"amount\": $((5000 + i * 1000)),
        \"paymentMethod\": \"TOSS_PAY\",
        \"description\": \"성능테스트 결제 $i\"
      }")
    
    END_TIME=$(date +%s%N)
    RESPONSE_TIME=$(( ($END_TIME - $START_TIME) / 1000000 ))
    
    SUCCESS=$(echo $RESPONSE | grep -o '"success":[^,]*' | cut -d':' -f2)
    TRANSACTION_ID=$(echo $RESPONSE | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4)
    
    echo "  - 결제 $i: ${RESPONSE_TIME}ms, 성공: $SUCCESS, TXN: $TRANSACTION_ID"
    
    # 0.5초 대기
    sleep 0.5
done

# Rate Limiting 테스트
echo ""
echo "4. Rate Limiting 테스트 - 빠른 요청 10회..."
for i in {1..10}; do
    RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" \
      -X GET http://localhost:8080/api/accounts/4/balance \
      -H "Authorization: Bearer $TOKEN")
    
    HTTP_CODE=$(echo $RESPONSE | grep -o 'HTTP_CODE:[0-9]*' | cut -d':' -f2)
    
    if [ "$HTTP_CODE" = "429" ]; then
        echo "  - 요청 $i: Rate Limited (HTTP $HTTP_CODE) [SUCCESS]"
        break
    else
        echo "  - 요청 $i: 정상 처리 (HTTP $HTTP_CODE)"
    fi
done

# 최종 모니터링 상태 확인
echo ""
echo "5. 테스트 후 시스템 상태..."
curl -s http://localhost:8080/api/admin/monitoring/stats

echo ""
echo ""
echo "6. 시스템 헬스 체크..."
curl -s http://localhost:8080/api/admin/monitoring/health

echo ""
echo ""
echo "7. 알림 확인..."
curl -s http://localhost:8080/api/admin/monitoring/alerts

echo ""
echo ""
echo "[SUCCESS] 성능 및 보안 테스트 완료!"