-- 토스뱅크 결제 플랫폼 테스트 데이터 삽입

USE tossbank_payment;

-- 1. 테스트 사용자 생성
INSERT INTO users (created_at, updated_at, email, password, name, phone_number, status, failed_login_attempts, is_email_verified, is_phone_verified) VALUES
(NOW(), NOW(), 'demo@tossbank.com', '$2a$10$9GKm7OgE2TlE8xV6TlE8xV6TlE8xV6TlE8xV6TlE8xV6TlE8xV6TlE', '데모 사용자', '010-9999-8888', 'ACTIVE', 0, 1, 1),
(NOW(), NOW(), 'user1@tossbank.com', '$2a$10$9GKm7OgE2TlE8xV6TlE8xV6TlE8xV6TlE8xV6TlE8xV6TlE8xV6TlE', '김토스', '010-1234-5678', 'ACTIVE', 0, 1, 1),
(NOW(), NOW(), 'user2@tossbank.com', '$2a$10$9GKm7OgE2TlE8xV6TlE8xV6TlE8xV6TlE8xV6TlE8xV6TlE8xV6TlE', '이뱅크', '010-2345-6789', 'ACTIVE', 0, 1, 1);

-- 2. 가맹점 생성
INSERT INTO merchants (created_at, updated_at, merchant_code, merchant_name, business_number, representative_name, phone_number, email, address, category, status, api_key, webhook_url) VALUES
(NOW(), NOW(), 'TOSS_STORE_001', '토스 온라인몰', '123-45-67890', '김사장', '02-1234-5678', 'store@tossbank.com', '서울시 강남구 테헤란로 123', 'ONLINE', 'ACTIVE', 'toss_api_key_1234567890abcdef1234567890abcdef12345678', 'https://store.tossbank.com/webhook'),
(NOW(), NOW(), 'TOSS_CAFE_002', '토스 카페', '234-56-78901', '박대표', '02-2345-6789', 'cafe@tossbank.com', '서울시 서초구 강남대로 456', 'RESTAURANT', 'ACTIVE', 'toss_api_key_abcdef1234567890abcdef1234567890abcdef12', 'https://cafe.tossbank.com/webhook');

-- 3. 계좌 생성
INSERT INTO accounts (created_at, updated_at, account_number, account_name, balance, account_type, status, daily_limit, monthly_limit, user_id) VALUES
(NOW(), NOW(), '1000-01-123456', '데모 입출금 계좌', 1500000.00, 'CHECKING', 'ACTIVE', 5000000.00, 50000000.00, 1),
(NOW(), NOW(), '1000-01-234567', '김토스 적금 계좌', 850000.00, 'SAVINGS', 'ACTIVE', 3000000.00, 30000000.00, 2),
(NOW(), NOW(), '1000-01-345678', '이뱅크 입출금 계좌', 2750000.00, 'CHECKING', 'ACTIVE', 5000000.00, 50000000.00, 3);

-- 4. 결제 거래 생성
INSERT INTO payments (created_at, updated_at, transaction_id, merchant_order_id, amount, description, payment_method, status, approved_at, client_ip, user_agent, user_id, account_id, merchant_id) VALUES
(NOW(), NOW(), 'TXN_20250903_001', 'ORDER_20250903_001', 89000.00, '스마트폰 케이스 구매', 'TOSS_PAY', 'COMPLETED', NOW(), '192.168.1.100', 'Mozilla/5.0', 1, 1, 1),
(NOW(), NOW(), 'TXN_20250903_002', 'ORDER_20250903_002', 4500.00, '아메리카노', 'ACCOUNT_TRANSFER', 'COMPLETED', NOW(), '192.168.1.101', 'Mozilla/5.0', 2, 2, 2),
(NOW(), NOW(), 'TXN_20250903_003', 'ORDER_20250903_003', 125000.00, '노트북 스탠드', 'CARD', 'PROCESSING', NULL, '192.168.1.102', 'Mozilla/5.0', 1, 1, 1),
(NOW(), NOW(), 'TXN_20250903_004', 'ORDER_20250903_004', 15000.00, '샐러드', 'TOSS_PAY', 'FAILED', NULL, '192.168.1.103', 'Mozilla/5.0', 3, 3, 2);

-- 5. 결제 이력 생성
INSERT INTO payment_histories (created_at, updated_at, amount, current_status, previous_status, description, processed_by, client_ip, payment_id) VALUES
(NOW(), NOW(), 89000.00, 'PENDING', NULL, '결제 요청 생성', 'SYSTEM', '192.168.1.100', 1),
(NOW(), NOW(), 89000.00, 'PROCESSING', 'PENDING', '결제 처리 시작', 'SYSTEM', '192.168.1.100', 1),
(NOW(), NOW(), 89000.00, 'COMPLETED', 'PROCESSING', '결제 완료', 'SYSTEM', '192.168.1.100', 1),

(NOW(), NOW(), 4500.00, 'PENDING', NULL, '결제 요청 생성', 'SYSTEM', '192.168.1.101', 2),
(NOW(), NOW(), 4500.00, 'COMPLETED', 'PENDING', '계좌이체 즉시 완료', 'SYSTEM', '192.168.1.101', 2),

(NOW(), NOW(), 125000.00, 'PENDING', NULL, '결제 요청 생성', 'SYSTEM', '192.168.1.102', 3),
(NOW(), NOW(), 125000.00, 'PROCESSING', 'PENDING', '카드 승인 처리 중', 'SYSTEM', '192.168.1.102', 3),

(NOW(), NOW(), 15000.00, 'PENDING', NULL, '결제 요청 생성', 'SYSTEM', '192.168.1.103', 4),
(NOW(), NOW(), 15000.00, 'FAILED', 'PENDING', '결제 실패 - 잔액 부족', 'SYSTEM', '192.168.1.103', 4);

-- 데이터 삽입 완료 확인
SELECT 'Users:', COUNT(*) as count FROM users
UNION ALL
SELECT 'Merchants:', COUNT(*) FROM merchants  
UNION ALL
SELECT 'Accounts:', COUNT(*) FROM accounts
UNION ALL
SELECT 'Payments:', COUNT(*) FROM payments
UNION ALL
SELECT 'Payment Histories:', COUNT(*) FROM payment_histories;