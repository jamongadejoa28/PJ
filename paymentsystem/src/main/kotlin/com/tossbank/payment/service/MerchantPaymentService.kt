package com.tossbank.payment.service

import com.tossbank.payment.dto.*
import com.tossbank.payment.entity.*
import com.tossbank.payment.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class MerchantPaymentService {

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

    private val secureRandom = SecureRandom()
    private val paymentKeyMap = mutableMapOf<String, String>() // In-memory store for demo

    fun requestPayment(
        apiKey: String, 
        request: MerchantPaymentRequest,
        clientIp: String
    ): ApiResponse<MerchantPaymentResponse> {
        // Find merchant by API key
        val merchant = merchantRepository.findByApiKey(apiKey)
            ?: return ApiResponse(
                success = false,
                message = "유효하지 않은 API 키입니다."
            )

        // Check merchant status
        if (merchant.status != MerchantStatus.ACTIVE) {
            return ApiResponse(
                success = false,
                message = "비활성화된 가맹점입니다."
            )
        }

        // Find user by email
        val user = userRepository.findByEmail(request.userEmail)
            ?: return ApiResponse(
                success = false,
                message = "사용자를 찾을 수 없습니다."
            )

        // Get user's primary account (first active account)
        val accounts = accountRepository.findActiveAccountsByUserId(user.id)
        if (accounts.isEmpty()) {
            return ApiResponse(
                success = false,
                message = "사용자의 활성 계좌를 찾을 수 없습니다."
            )
        }

        val account = accounts.first() // Use first account as primary

        // Create payment
        val transactionId = generateTransactionId()
        val paymentKey = generatePaymentKey()
        
        val payment = Payment(
            transactionId = transactionId,
            merchantOrderId = request.merchantOrderId,
            amount = request.amount,
            description = request.description,
            paymentMethod = request.paymentMethod,
            status = PaymentStatus.PENDING,
            clientIp = clientIp,
            userAgent = "Merchant-API"
        )

        payment.user = user
        payment.account = account
        payment.merchant = merchant

        val savedPayment = paymentRepository.save(payment)

        // Store payment key mapping
        paymentKeyMap[paymentKey] = transactionId

        // Create initial payment history
        createPaymentHistory(savedPayment, null, PaymentStatus.PENDING.name, "가맹점 결제 요청 생성")

        val checkoutUrl = generateCheckoutUrl(paymentKey, request)

        return ApiResponse(
            success = true,
            message = "결제 요청이 생성되었습니다.",
            data = MerchantPaymentResponse(
                transactionId = transactionId,
                paymentKey = paymentKey,
                checkoutUrl = checkoutUrl,
                status = PaymentStatus.PENDING,
                amount = request.amount,
                merchantOrderId = request.merchantOrderId
            )
        )
    }

    fun confirmPayment(apiKey: String, request: PaymentCallbackRequest): ApiResponse<PaymentResponse> {
        // Find merchant by API key
        val merchant = merchantRepository.findByApiKey(apiKey)
            ?: return ApiResponse(
                success = false,
                message = "유효하지 않은 API 키입니다."
            )

        // Get transaction ID from payment key
        val transactionId = paymentKeyMap[request.paymentKey]
            ?: return ApiResponse(
                success = false,
                message = "유효하지 않은 결제 키입니다."
            )

        // Find payment
        val payment = paymentRepository.findByTransactionId(transactionId)
            ?: return ApiResponse(
                success = false,
                message = "결제 내역을 찾을 수 없습니다."
            )

        // Verify merchant ownership
        if (payment.merchant.id != merchant.id) {
            return ApiResponse(
                success = false,
                message = "결제 내역에 접근할 권한이 없습니다."
            )
        }

        // Verify order ID and amount
        if (payment.merchantOrderId != request.orderId) {
            return ApiResponse(
                success = false,
                message = "주문 ID가 일치하지 않습니다."
            )
        }

        if (payment.amount.compareTo(request.amount) != 0) {
            return ApiResponse(
                success = false,
                message = "결제 금액이 일치하지 않습니다."
            )
        }

        // Check if payment is still pending
        if (!payment.isPending()) {
            return ApiResponse(
                success = false,
                message = "이미 처리된 결제입니다."
            )
        }

        // Process payment
        val result = processPayment(payment)

        val finalPayment = paymentRepository.findById(payment.id).get()

        return ApiResponse(
            success = result,
            message = if (result) "결제가 완료되었습니다." else finalPayment.failureReason ?: "결제에 실패했습니다.",
            data = PaymentResponse(
                transactionId = finalPayment.transactionId,
                merchantOrderId = finalPayment.merchantOrderId,
                amount = finalPayment.amount,
                status = finalPayment.status,
                paymentMethod = finalPayment.paymentMethod,
                description = finalPayment.description,
                approvedAt = finalPayment.approvedAt,
                createdAt = finalPayment.createdAt
            )
        )
    }

    @Transactional(readOnly = true)
    fun getPaymentStatus(apiKey: String, transactionId: String): ApiResponse<PaymentResponse> {
        // Find merchant by API key
        val merchant = merchantRepository.findByApiKey(apiKey)
            ?: return ApiResponse(
                success = false,
                message = "유효하지 않은 API 키입니다."
            )

        // Find payment
        val payment = paymentRepository.findByTransactionId(transactionId)
            ?: return ApiResponse(
                success = false,
                message = "결제 내역을 찾을 수 없습니다."
            )

        // Verify merchant ownership
        if (payment.merchant.id != merchant.id) {
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

    fun cancelPayment(apiKey: String, transactionId: String, reason: String): ApiResponse<String> {
        // Find merchant by API key
        val merchant = merchantRepository.findByApiKey(apiKey)
            ?: return ApiResponse(
                success = false,
                message = "유효하지 않은 API 키입니다."
            )

        // Find payment
        val payment = paymentRepository.findByTransactionId(transactionId)
            ?: return ApiResponse(
                success = false,
                message = "결제 내역을 찾을 수 없습니다."
            )

        // Verify merchant ownership
        if (payment.merchant.id != merchant.id) {
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

    private fun processPayment(payment: Payment): Boolean {
        return try {
            when (payment.paymentMethod) {
                PaymentMethod.ACCOUNT_TRANSFER -> {
                    createPaymentHistory(payment, PaymentStatus.PENDING.name, PaymentStatus.PROCESSING.name, "계좌이체 처리 시작")
                    
                    // Check balance
                    if (payment.account.balance < payment.amount) {
                        payment.fail("잔액이 부족합니다.")
                        paymentRepository.save(payment)
                        createPaymentHistory(payment, PaymentStatus.PROCESSING.name, PaymentStatus.FAILED.name, "잔액 부족")
                        return false
                    }
                    
                    // Withdraw from account
                    payment.account.withdraw(payment.amount)
                    accountRepository.save(payment.account)
                    
                    payment.approve()
                    paymentRepository.save(payment)
                    createPaymentHistory(payment, PaymentStatus.PROCESSING.name, PaymentStatus.COMPLETED.name, "계좌이체 완료")
                    true
                }
                PaymentMethod.TOSS_PAY -> {
                    createPaymentHistory(payment, PaymentStatus.PENDING.name, PaymentStatus.PROCESSING.name, "토스페이 처리 시작")
                    Thread.sleep(500) // Simulate processing
                    payment.approve()
                    paymentRepository.save(payment)
                    createPaymentHistory(payment, PaymentStatus.PROCESSING.name, PaymentStatus.COMPLETED.name, "토스페이 결제 완료")
                    true
                }
                PaymentMethod.CARD -> {
                    createPaymentHistory(payment, PaymentStatus.PENDING.name, PaymentStatus.PROCESSING.name, "카드 결제 처리 시작")
                    Thread.sleep(1000) // Simulate processing
                    payment.approve()
                    paymentRepository.save(payment)
                    createPaymentHistory(payment, PaymentStatus.PROCESSING.name, PaymentStatus.COMPLETED.name, "카드 결제 완료")
                    true
                }
                else -> {
                    payment.fail("지원하지 않는 결제 수단입니다.")
                    paymentRepository.save(payment)
                    createPaymentHistory(payment, PaymentStatus.PENDING.name, PaymentStatus.FAILED.name, "지원하지 않는 결제 수단")
                    false
                }
            }
        } catch (e: Exception) {
            payment.fail("결제 처리 중 오류가 발생했습니다: ${e.message}")
            paymentRepository.save(payment)
            createPaymentHistory(payment, PaymentStatus.PROCESSING.name, PaymentStatus.FAILED.name, e.message)
            false
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
            processedBy = "MERCHANT-API",
            clientIp = payment.clientIp
        )
        paymentHistoryRepository.save(history)
    }

    private fun generateTransactionId(): String {
        val timestamp = System.currentTimeMillis()
        val random = secureRandom.nextInt(100000)
        return "MTXN${timestamp}${random}"
    }

    private fun generatePaymentKey(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    private fun generateCheckoutUrl(paymentKey: String, request: MerchantPaymentRequest): String {
        return "https://checkout.tossbank.com/payments/${paymentKey}?orderId=${request.merchantOrderId}&amount=${request.amount}"
    }
}