package com.tossbank.payment.dto

import com.tossbank.payment.entity.PaymentMethod
import com.tossbank.payment.entity.PaymentStatus
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentRequest(
    @field:NotNull(message = "계좌 ID를 입력해주세요")
    val accountId: Long,
    
    @field:NotBlank(message = "가맹점 코드를 입력해주세요")
    val merchantCode: String,
    
    @field:NotBlank(message = "가맹점 주문 ID를 입력해주세요")
    val merchantOrderId: String,
    
    @field:NotNull(message = "결제 금액을 입력해주세요")
    @field:DecimalMin(value = "100", message = "최소 결제 금액은 100원입니다")
    @field:DecimalMax(value = "50000000", message = "최대 결제 금액은 5,000만원입니다")
    val amount: BigDecimal,
    
    @field:NotNull(message = "결제 수단을 선택해주세요")
    val paymentMethod: PaymentMethod = PaymentMethod.ACCOUNT_TRANSFER,
    
    @field:Size(max = 200, message = "결제 설명은 200자 이하여야 합니다")
    val description: String? = null,
    
    val clientIp: String? = null,
    val userAgent: String? = null
)

data class PaymentResponse(
    val transactionId: String,
    val merchantOrderId: String,
    val amount: BigDecimal,
    val status: PaymentStatus,
    val paymentMethod: PaymentMethod,
    val description: String?,
    val approvedAt: LocalDateTime?,
    val createdAt: LocalDateTime
)

data class PaymentSummary(
    val transactionId: String,
    val merchantOrderId: String,
    val merchantName: String,
    val amount: BigDecimal,
    val status: PaymentStatus,
    val paymentMethod: PaymentMethod,
    val description: String?,
    val createdAt: LocalDateTime
)

data class PaymentCancelRequest(
    @field:NotBlank(message = "취소 사유를 입력해주세요")
    @field:Size(max = 200, message = "취소 사유는 200자 이하여야 합니다")
    val reason: String
)

// Merchant API용 결제 요청
data class MerchantPaymentRequest(
    @field:NotBlank(message = "가맹점 주문 ID를 입력해주세요")
    val merchantOrderId: String,
    
    @field:NotNull(message = "결제 금액을 입력해주세요")
    @field:DecimalMin(value = "100", message = "최소 결제 금액은 100원입니다")
    val amount: BigDecimal,
    
    @field:NotNull(message = "결제 수단을 선택해주세요")
    val paymentMethod: PaymentMethod,
    
    @field:NotBlank(message = "사용자 이메일을 입력해주세요")
    @field:Email(message = "올바른 이메일 형식을 입력해주세요")
    val userEmail: String,
    
    @field:Size(max = 200, message = "결제 설명은 200자 이하여야 합니다")
    val description: String? = null,
    
    @field:Size(max = 200, message = "성공 URL은 200자 이하여야 합니다")
    val successUrl: String? = null,
    
    @field:Size(max = 200, message = "실패 URL은 200자 이하여야 합니다")
    val failUrl: String? = null
)

data class MerchantPaymentResponse(
    val transactionId: String,
    val paymentKey: String, // 결제 키 (결제 완료 시 사용)
    val checkoutUrl: String?, // 결제 페이지 URL
    val status: PaymentStatus,
    val amount: BigDecimal,
    val merchantOrderId: String
)

data class PaymentCallbackRequest(
    @field:NotBlank(message = "결제 키를 입력해주세요")
    val paymentKey: String,
    
    @field:NotBlank(message = "가맹점 주문 ID를 입력해주세요")
    val orderId: String,
    
    @field:NotNull(message = "결제 금액을 입력해주세요")
    val amount: BigDecimal
)