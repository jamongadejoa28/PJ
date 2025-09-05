package com.tossbank.payment.service

import com.tossbank.payment.dto.*
import com.tossbank.payment.entity.*
import com.tossbank.payment.repository.*
import com.tossbank.payment.exception.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Propagation
import org.springframework.scheduling.annotation.Async
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.redis.core.RedisTemplate
import java.math.BigDecimal
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@Service
class OptimizedPaymentService {

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var merchantRepository: MerchantRepository

    @Autowired
    private lateinit var paymentHistoryRepository: PaymentHistoryRepository

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    private val secureRandom = SecureRandom()

    /**
     * 최적화된 결제 처리 메소드
     * - 트랜잭션 범위 최소화
     * - 비동기 히스토리 처리
     * - 불필요한 DB 조회 제거
     */
    @Transactional(readOnly = true)
    fun processPaymentOptimized(userId: Long, request: PaymentRequest): ApiResponse<PaymentResponse> {
        // 1. 빠른 검증 (읽기 전용)
        val validationResult = validatePaymentRequest(userId, request)
        if (!validationResult.success) {
            return validationResult
        }

        // 2. 핵심 결제 처리만 트랜잭션으로 분리
        val payment = executePaymentTransaction(userId, request)

        // 3. 비동기로 히스토리 및 후속 작업 처리
        processPaymentHistoryAsync(payment)
        updateCacheAsync(payment)

        // 4. 즉시 응답 반환 (DB 재조회 없이)
        return ApiResponse(
            success = payment.status == PaymentStatus.COMPLETED,
            message = getStatusMessage(payment),
            data = PaymentResponse(
                transactionId = payment.transactionId,
                merchantOrderId = payment.merchantOrderId,
                amount = payment.amount,
                status = payment.status,
                paymentMethod = payment.paymentMethod,
                description = payment.description,
                approvedAt = payment.approvedAt,
                createdAt = payment.createdAt
            )
        )
    }

    /**
     * 결제 요청 검증 (읽기 전용, 빠른 실행)
     */
    @Transactional(readOnly = true)
    private fun validatePaymentRequest(userId: Long, request: PaymentRequest): ApiResponse<PaymentResponse> {
        // 사용자 검증 (캐시 활용)
        val user = getCachedUser(userId) 
            ?: return ApiResponse(false, "사용자를 찾을 수 없습니다.")

        // 계좌 검증 (캐시 활용)
        val account = getCachedAccount(request.accountId)
            ?: return ApiResponse(false, "계좌를 찾을 수 없습니다.")

        if (account.user.id != userId) {
            return ApiResponse(false, "계좌에 대한 접근 권한이 없습니다.")
        }

        // 가맹점 검증 (캐시 활용)
        val merchant = getCachedMerchant(request.merchantCode)
            ?: return ApiResponse(false, "가맹점을 찾을 수 없습니다.")

        if (merchant.status != MerchantStatus.ACTIVE) {
            return ApiResponse(false, "비활성화된 가맹점입니다.")
        }

        // 금액 검증
        if (request.amount <= BigDecimal.ZERO) {
            return ApiResponse(false, "결제 금액은 0보다 커야 합니다.")
        }

        // 잔액 및 한도 검증 (계좌이체의 경우만)
        if (request.paymentMethod == PaymentMethod.ACCOUNT_TRANSFER) {
            if (account.balance < request.amount) {
                return ApiResponse(false, "잔액이 부족합니다.")
            }
            if (request.amount > account.dailyLimit) {
                return ApiResponse(false, "일일 한도를 초과했습니다.")
            }
        }

        return ApiResponse(true, "검증 성공")
    }

    /**
     * 핵심 결제 처리 (최소 트랜잭션 범위)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private fun executePaymentTransaction(userId: Long, request: PaymentRequest): Payment {
        val user = userRepository.findById(userId).get()
        val account = accountRepository.findById(request.accountId).get()
        val merchant = merchantRepository.findByMerchantCode(request.merchantCode)!!

        // 결제 객체 생성 및 저장
        val payment = Payment(
            transactionId = generateTransactionId(),
            merchantOrderId = request.merchantOrderId,
            amount = request.amount,
            description = request.description,
            paymentMethod = request.paymentMethod,
            status = PaymentStatus.PROCESSING,  // 바로 처리 상태로
            clientIp = request.clientIp,
            userAgent = request.userAgent
        )

        payment.user = user
        payment.account = account  
        payment.merchant = merchant

        // 결제 방법별 처리
        when (request.paymentMethod) {
            PaymentMethod.ACCOUNT_TRANSFER -> {
                // 잔액 차감 (원자적 연산)
                account.balance = account.balance.subtract(request.amount)
                accountRepository.save(account)
                
                payment.approve()
                payment.approvedAt = LocalDateTime.now()
            }
            PaymentMethod.TOSS_PAY -> {
                // 토스페이 처리 시뮬레이션 (빠른 승인)
                payment.approve()
                payment.approvedAt = LocalDateTime.now()
            }
            PaymentMethod.CARD -> {
                // 카드 처리 시뮬레이션
                if (secureRandom.nextInt(100) < 95) { // 95% 성공률
                    payment.approve()
                    payment.approvedAt = LocalDateTime.now()
                } else {
                    payment.fail("카드 승인 실패")
                }
            }
            else -> {
                payment.fail("지원하지 않는 결제 수단입니다.")
            }
        }

        return paymentRepository.save(payment)
    }

    /**
     * 비동기 히스토리 처리 (메인 플로우와 분리)
     */
    @Async
    fun processPaymentHistoryAsync(payment: Payment) {
        try {
            val histories = mutableListOf<PaymentHistory>()
            
            // 요청 생성 히스토리
            histories.add(PaymentHistory(
                amount = payment.amount,
                currentStatus = "PROCESSING",
                previousStatus = null,
                description = "결제 처리 시작",
                processedBy = "SYSTEM",
                clientIp = payment.clientIp
            ).apply { this.payment = payment })

            // 최종 상태 히스토리
            histories.add(PaymentHistory(
                amount = payment.amount,
                currentStatus = payment.status.name,
                previousStatus = "PROCESSING", 
                description = if (payment.status == PaymentStatus.COMPLETED) "결제 완료" else "결제 실패: ${payment.failureReason}",
                processedBy = "SYSTEM",
                clientIp = payment.clientIp
            ).apply { this.payment = payment })

            // 배치로 저장 (성능 개선)
            paymentHistoryRepository.saveAll(histories)
        } catch (ex: Exception) {
            // 히스토리 저장 실패가 결제에 영향주지 않도록
            println("Failed to save payment history: ${ex.message}")
        }
    }

    /**
     * 비동기 캐시 업데이트
     */
    @Async  
    fun updateCacheAsync(payment: Payment) {
        try {
            // 사용자 캐시 무효화
            redisTemplate.delete("user:${payment.user.id}")
            // 계좌 캐시 무효화  
            redisTemplate.delete("account:${payment.account!!.id}")
        } catch (ex: Exception) {
            println("Failed to update cache: ${ex.message}")
        }
    }

    /**
     * 캐시된 사용자 조회
     */
    @Cacheable("users")
    fun getCachedUser(userId: Long): User? {
        return userRepository.findById(userId).orElse(null)
    }

    /**
     * 캐시된 계좌 조회
     */
    @Cacheable("accounts") 
    fun getCachedAccount(accountId: Long): Account? {
        return accountRepository.findById(accountId).orElse(null)
    }

    /**
     * 캐시된 가맹점 조회
     */
    @Cacheable("merchants")
    fun getCachedMerchant(merchantCode: String): Merchant? {
        return merchantRepository.findByMerchantCode(merchantCode)
    }

    private fun generateTransactionId(): String {
        return "TXN${System.nanoTime()}${secureRandom.nextInt(1000)}"
    }

    private fun getStatusMessage(payment: Payment): String {
        return when (payment.status) {
            PaymentStatus.COMPLETED -> "결제가 완료되었습니다."
            PaymentStatus.FAILED -> payment.failureReason ?: "결제에 실패했습니다."
            else -> "결제가 처리 중입니다."
        }
    }
}