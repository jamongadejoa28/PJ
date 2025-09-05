-- 토스뱅크 결제 플랫폼 초기 데이터베이스 스키마
-- 이 파일은 컨테이너 시작 시 자동으로 실행됩니다

USE tossbank_payment;

-- 테스트용 초기 데이터 삽입
-- 실제 운영에서는 제거해야 합니다

-- 1. 테스트 사용자 생성
INSERT INTO users (email, password, name, phone_number, status, created_at, updated_at) VALUES
('admin@tossbank.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6M1N0M4XUX7.m4UM6g.ER5O', '관리자', '010-0000-0000', 'ACTIVE', NOW(), NOW()),
('user1@tossbank.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6M1N0M4XUX7.m4UM6g.ER5O', '홍길동', '010-1234-5678', 'ACTIVE', NOW(), NOW()),
('user2@tossbank.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6M1N0M4XUX7.m4UM6g.ER5O', '김영희', '010-9876-5432', 'ACTIVE', NOW(), NOW());

-- 2. 테스트 가맹점 생성
INSERT INTO merchants (merchant_code, merchant_name, business_number, representative_name, phone_number, email, address, category, status, api_key, created_at, updated_at) VALUES
('TOSS001', '토스커피', '123-45-67890', '이지은', '02-1234-5678', 'coffee@toss.im', '서울시 강남구 테헤란로 123', 'RESTAURANT', 'ACTIVE', 'test_api_key_123456789', NOW(), NOW()),
('TOSS002', '토스마트', '987-65-43210', '박민수', '02-9876-5432', 'mart@toss.im', '서울시 서초구 반포대로 456', 'RETAIL', 'ACTIVE', 'test_api_key_987654321', NOW(), NOW());

-- 3. 테스트 계좌 생성 (사용자1용)
INSERT INTO accounts (account_number, account_name, balance, account_type, status, daily_limit, monthly_limit, user_id, created_at, updated_at) VALUES
('1001234567890', '홍길동 입출금계좌', 1000000.00, 'CHECKING', 'ACTIVE', 5000000.00, 50000000.00, 2, NOW(), NOW()),
('1001234567891', '홍길동 적금계좌', 5000000.00, 'SAVINGS', 'ACTIVE', 3000000.00, 30000000.00, 2, NOW(), NOW());

-- 4. 테스트 계좌 생성 (사용자2용)
INSERT INTO accounts (account_number, account_name, balance, account_type, status, daily_limit, monthly_limit, user_id, created_at, updated_at) VALUES
('1009876543210', '김영희 입출금계좌', 2000000.00, 'CHECKING', 'ACTIVE', 5000000.00, 50000000.00, 3, NOW(), NOW());

-- 5. 테스트 결제 내역 생성
INSERT INTO payments (transaction_id, merchant_order_id, amount, description, payment_method, status, user_id, account_id, merchant_id, created_at, updated_at) VALUES
('TXN1693708800001', 'ORDER_20240903_001', 5000.00, '아메리카노 2잔', 'ACCOUNT_TRANSFER', 'COMPLETED', 2, 1, 1, NOW(), NOW()),
('TXN1693708800002', 'ORDER_20240903_002', 15000.00, '생필품 구매', 'ACCOUNT_TRANSFER', 'COMPLETED', 2, 1, 2, NOW(), NOW()),
('TXN1693708800003', 'ORDER_20240903_003', 8000.00, '카페라떼', 'ACCOUNT_TRANSFER', 'PENDING', 3, 3, 1, NOW(), NOW());

-- 6. 결제 히스토리 생성
INSERT INTO payment_histories (previous_status, current_status, amount, description, payment_id, created_at, updated_at) VALUES
(NULL, 'PENDING', 5000.00, '결제 요청', 1, NOW(), NOW()),
('PENDING', 'COMPLETED', 5000.00, '결제 승인', 1, NOW(), NOW()),
(NULL, 'PENDING', 15000.00, '결제 요청', 2, NOW(), NOW()),
('PENDING', 'COMPLETED', 15000.00, '결제 승인', 2, NOW(), NOW()),
(NULL, 'PENDING', 8000.00, '결제 요청', 3, NOW(), NOW());

-- 인덱스 생성 (성능 최적화)
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone_number);
CREATE INDEX idx_users_status ON users(status);

CREATE INDEX idx_accounts_number ON accounts(account_number);
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_status ON accounts(status);

CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_merchant_id ON payments(merchant_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);

CREATE INDEX idx_payment_histories_payment_id ON payment_histories(payment_id);
CREATE INDEX idx_payment_histories_created_at ON payment_histories(created_at);

CREATE INDEX idx_merchants_code ON merchants(merchant_code);
CREATE INDEX idx_merchants_api_key ON merchants(api_key);
CREATE INDEX idx_merchants_status ON merchants(status);

-- 통계용 뷰 생성
CREATE VIEW v_user_account_summary AS
SELECT 
    u.id as user_id,
    u.name as user_name,
    u.email,
    COUNT(a.id) as account_count,
    COALESCE(SUM(a.balance), 0) as total_balance
FROM users u
LEFT JOIN accounts a ON u.id = a.user_id AND a.status = 'ACTIVE'
WHERE u.status = 'ACTIVE'
GROUP BY u.id, u.name, u.email;

CREATE VIEW v_payment_daily_summary AS
SELECT 
    DATE(created_at) as payment_date,
    status,
    COUNT(*) as transaction_count,
    SUM(amount) as total_amount,
    AVG(amount) as avg_amount
FROM payments
GROUP BY DATE(created_at), status
ORDER BY payment_date DESC;

-- 샘플 쿼리 주석
-- 사용자별 계좌 요약: SELECT * FROM v_user_account_summary;
-- 일별 결제 현황: SELECT * FROM v_payment_daily_summary WHERE payment_date = CURDATE();
-- 최근 거래 내역: SELECT * FROM payments ORDER BY created_at DESC LIMIT 10;
-- 사용자별 거래 내역: SELECT * FROM payments WHERE user_id = 2 ORDER BY created_at DESC;