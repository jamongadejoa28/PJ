package com.tossbank.payment.service

import com.tossbank.payment.dto.*
import com.tossbank.payment.entity.*
import com.tossbank.payment.repository.*
import com.tossbank.payment.exception.*
import com.tossbank.payment.event.PaymentEventPublisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
@Transactional
class PaymentService {

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
    private lateinit var paymentEventPublisher: PaymentEventPublisher

    private val secureRandom = SecureRandom()

    fun processPayment(userId: Long, request: PaymentRequest): ApiResponse<PaymentResponse> {
        // Find user with proper exception handling
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다.", "User", userId.toString()) }

        // Find account with proper exception handling
        val account = accountRepository.findById(request.accountId)
            .orElseThrow { AccountNotFoundException("계좌 ID ${request.accountId}를 찾을 수 없습니다.") }

        // Check if account belongs to user
        if (account.user.id != userId) {
            throw UnauthorizedAccessException("계좌 ID ${request.accountId}에 대한 접근 권한이 없습니다.")
        }

        // Find merchant with proper exception handling
        val merchant = merchantRepository.findByMerchantCode(request.merchantCode)
            ?: throw MerchantNotFoundException("가맹점 코드 ${request.merchantCode}를 찾을 수 없습니다.")

        // Check if merchant is active
        if (merchant.status != MerchantStatus.ACTIVE) {
            throw MerchantInactiveException("가맹점 ${request.merchantCode}가 비활성 상태입니다.")
        }

        // Validate payment amount
        if (request.amount <= BigDecimal.ZERO) {
            throw PaymentAmountInvalidException("결제 금액은 0보다 커야 합니다. 요청 금액: ₩${request.amount}")
        }

        // Check account balance for account transfer
        if (request.paymentMethod == PaymentMethod.ACCOUNT_TRANSFER) {
            if (account.balance < request.amount) {
                throw InsufficientBalanceException(
                    "계좌 잔액이 부족합니다.",
                    request.amount,
                    account.balance
                )
            }

            // Check daily limit
            if (request.amount > account.dailyLimit) {
                throw AccountLimitExceededException(
                    "일일 한도를 초과했습니다.",
                    "DAILY_LIMIT",
                    account.dailyLimit,
                    request.amount
                )
            }
        }

        // Create payment
        val transactionId = generateTransactionId()
        val payment = Payment(
            transactionId = transactionId,
            merchantOrderId = request.merchantOrderId,
            amount = request.amount,
            description = request.description,
            paymentMethod = request.paymentMethod,
            status = PaymentStatus.PENDING,
            clientIp = request.clientIp,
            userAgent = request.userAgent
        )

        payment.user = user
        payment.account = account
        payment.merchant = merchant

        val savedPayment = paymentRepository.save(payment)

        // Create initial payment history
        createPaymentHistory(savedPayment, null, PaymentStatus.PENDING.name, "결제 요청 생성")

        // Process payment based on method
        val result = when (request.paymentMethod) {
            PaymentMethod.ACCOUNT_TRANSFER -> processAccountTransfer(savedPayment)
            PaymentMethod.TOSS_PAY -> processTossPay(savedPayment)
            PaymentMethod.CARD -> processCard(savedPayment)
            else -> {
                savedPayment.fail("지원하지 않는 결제 수단입니다.")
                paymentRepository.save(savedPayment)
                createPaymentHistory(savedPayment, PaymentStatus.PENDING.name, PaymentStatus.FAILED.name, "지원하지 않는 결제 수단")
                false
            }
        }

        // 불필요한 DB 재조회 제거 - 성능 최적화
        return ApiResponse(
            success = result,
            message = if (result) "결제가 완료되었습니다." else savedPayment.failureReason ?: "결제에 실패했습니다.",
            data = PaymentResponse(
                transactionId = savedPayment.transactionId,
                merchantOrderId = savedPayment.merchantOrderId,
                amount = savedPayment.amount,
                status = savedPayment.status,
                paymentMethod = savedPayment.paymentMethod,
                description = savedPayment.description,
                approvedAt = savedPayment.approvedAt,
                createdAt = savedPayment.createdAt
            )
        )
    }

    private fun processAccountTransfer(payment: Payment): Boolean {
        return try {
            createPaymentHistory(payment, PaymentStatus.PENDING.name, PaymentStatus.PROCESSING.name, "계좌이체 처리 시작")
            
            // Withdraw from account
            payment.account.withdraw(payment.amount)
            accountRepository.save(payment.account)
            
            // Approve payment
            payment.approve()
            paymentRepository.save(payment)
            
            createPaymentHistory(payment, PaymentStatus.PROCESSING.name, PaymentStatus.COMPLETED.name, "계좌이체 완료")
            
            // Publish payment completed event to Kafka
            paymentEventPublisher.publishPaymentCompletedEvent(payment)
            
            // Send notification to user
            paymentEventPublisher.publishNotificationEvent(
                payment.transactionId,
                payment.user.id,
                "계좌이체 결제가 완료되었습니다. 금액: ₩${payment.amount}",
                "PUSH"
            )
            
            true
        } catch (e: Exception) {
            payment.fail("계좌이체 처리 중 오류가 발생했습니다: ${e.message}")
            paymentRepository.save(payment)
            createPaymentHistory(payment, PaymentStatus.PROCESSING.name, PaymentStatus.FAILED.name, e.message)
            
            // Publish payment failed event to Kafka
            paymentEventPublisher.publishPaymentFailedEvent(payment)
            
            // Send failure notification to user
            paymentEventPublisher.publishNotificationEvent(
                payment.transactionId,
                payment.user.id,
                "계좌이체 결제가 실패했습니다. 사유: ${e.message}",
                "EMAIL"
            )
            
            false
        }
    }

    private fun processTossPay(payment: Payment): Boolean {
        return try {
            createPaymentHistory(payment, PaymentStatus.PENDING.name, PaymentStatus.PROCESSING.name, "토스페이 처리 시작")
            
            // Simulate Toss Pay processing
            Thread.sleep(1000) // Simulate API call delay
            
            payment.approve()
            paymentRepository.save(payment)
            
            createPaymentHistory(payment, PaymentStatus.PROCESSING.name, PaymentStatus.COMPLETED.name, "토스페이 결제 완료")
            
            // Publish payment completed event to Kafka
            paymentEventPublisher.publishPaymentCompletedEvent(payment)
            
            // Send notification to user
            paymentEventPublisher.publishNotificationEvent(
                payment.transactionId,
                payment.user.id,
                "토스페이 결제가 완료되었습니다. 금액: ₩${payment.amount}",
                "SMS"
            )
            
            true
        } catch (e: Exception) {
            payment.fail("토스페이 처리 중 오류가 발생했습니다: ${e.message}")
            paymentRepository.save(payment)
            createPaymentHistory(payment, PaymentStatus.PROCESSING.name, PaymentStatus.FAILED.name, e.message)
            
            // Publish payment failed event to Kafka
            paymentEventPublisher.publishPaymentFailedEvent(payment)
            
            // Send failure notification to user
            paymentEventPublisher.publishNotificationEvent(
                payment.transactionId,
                payment.user.id,
                "토스페이 결제가 실패했습니다. 사유: ${e.message}",
                "SMS"
            )
            
            false
        }
    }

    private fun processCard(payment: Payment): Boolean {
        return try {
            createPaymentHistory(payment, PaymentStatus.PENDING.name, PaymentStatus.PROCESSING.name, "카드 결제 처리 시작")
            
            // Simulate card processing
            Thread.sleep(2000) // Simulate API call delay
            
            payment.approve()
            paymentRepository.save(payment)
            
            createPaymentHistory(payment, PaymentStatus.PROCESSING.name, PaymentStatus.COMPLETED.name, "카드 결제 완료")
            
            // Publish payment completed event to Kafka
            paymentEventPublisher.publishPaymentCompletedEvent(payment)
            
            // Send notification to user
            paymentEventPublisher.publishNotificationEvent(
                payment.transactionId,
                payment.user.id,
                "카드 결제가 완료되었습니다. 금액: ₩${payment.amount}",
                "EMAIL"
            )
            
            true
        } catch (e: Exception) {
            payment.fail("카드 결제 처리 중 오류가 발생했습니다: ${e.message}")
            paymentRepository.save(payment)
            createPaymentHistory(payment, PaymentStatus.PROCESSING.name, PaymentStatus.FAILED.name, e.message)
            
            // Publish payment failed event to Kafka
            paymentEventPublisher.publishPaymentFailedEvent(payment)
            
            // Send failure notification to user
            paymentEventPublisher.publishNotificationEvent(
                payment.transactionId,
                payment.user.id,
                "카드 결제가 실패했습니다. 사유: ${e.message}",
                "EMAIL"
            )
            
            false
        }
    }

    @Transactional(readOnly = true)
    fun getPayment(userId: Long, transactionId: String): ApiResponse<PaymentResponse> {
        val payment = paymentRepository.findByTransactionId(transactionId)
            ?: return ApiResponse(
                success = false,
                message = "결제 내역을 찾을 수 없습니다."
            )

        if (payment.user.id != userId) {
            return ApiResponse(
                success = false,
                message = "결제 내역에 접근할 권한이 없습니다."
            )
        }

        return ApiResponse(
            success = true,
            message = "결제 조회 성공",
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

    @Transactional(readOnly = true)
    fun getPayments(userId: Long): ApiResponse<List<PaymentSummary>> {
        val payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
        
        val paymentSummaries = payments.map { payment ->
            PaymentSummary(
                transactionId = payment.transactionId,
                merchantOrderId = payment.merchantOrderId,
                merchantName = payment.merchant.merchantName,
                amount = payment.amount,
                status = payment.status,
                paymentMethod = payment.paymentMethod,
                description = payment.description,
                createdAt = payment.createdAt
            )
        }

        return ApiResponse(
            success = true,
            message = "결제 목록 조회 성공",
            data = paymentSummaries
        )
    }

    fun cancelPayment(userId: Long, transactionId: String, reason: String): ApiResponse<String> {
        val payment = paymentRepository.findByTransactionId(transactionId)
            ?: return ApiResponse(
                success = false,
                message = "결제 내역을 찾을 수 없습니다."
            )

        if (payment.user.id != userId) {
            return ApiResponse(
                success = false,
                message = "결제 내역에 접근할 권한이 없습니다."
            )
        }

        if (!payment.isCompleted()) {
            return ApiResponse(
                success = false,
                message = "완료된 결제만 취소할 수 있습니다."
            )
        }

        return try {
            // Refund to account if it was account transfer
            if (payment.paymentMethod == PaymentMethod.ACCOUNT_TRANSFER) {
                payment.account.deposit(payment.amount)
                accountRepository.save(payment.account)
            }

            payment.cancel(reason)
            paymentRepository.save(payment)
            
            createPaymentHistory(payment, PaymentStatus.COMPLETED.name, PaymentStatus.CANCELLED.name, reason)

            ApiResponse(
                success = true,
                message = "결제가 취소되었습니다."
            )
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "결제 취소 중 오류가 발생했습니다: ${e.message}"
            )
        }
    }

    private fun createPaymentHistory(
        payment: Payment,
        previousStatus: String?,
        currentStatus: String,
        description: String?
    ) {
        val history = PaymentHistory.createStatusChangeHistory(
            payment = payment,
            previousStatus = previousStatus,
            currentStatus = currentStatus,
            description = description,
            processedBy = "SYSTEM",
            clientIp = payment.clientIp
        )
        paymentHistoryRepository.save(history)
    }

    private fun generateTransactionId(): String {
        val timestamp = System.currentTimeMillis()
        val random = secureRandom.nextInt(100000)
        return "TXN${timestamp}${random}"
    }
}