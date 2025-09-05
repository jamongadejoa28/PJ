package com.tossbank.payment.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "payments",
    indexes = [
        Index(name = "idx_payment_transaction_id", columnList = "transaction_id"),
        Index(name = "idx_payment_merchant_order_id", columnList = "merchant_order_id"),
        Index(name = "idx_payment_status", columnList = "status"),
        Index(name = "idx_payment_created_at", columnList = "created_at")
    ]
)
class Payment(
    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    var transactionId: String,

    @Column(name = "merchant_order_id", nullable = false, length = 64)
    var merchantOrderId: String,

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    var amount: BigDecimal,

    @Column(name = "description", length = 200)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    var paymentMethod: PaymentMethod = PaymentMethod.ACCOUNT_TRANSFER,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @Column(name = "failure_reason", length = 200)
    var failureReason: String? = null,

    @Column(name = "client_ip", length = 45)
    var clientIp: String? = null,

    @Column(name = "user_agent", length = 500)
    var userAgent: String? = null

) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    lateinit var merchant: Merchant

    fun approve() {
        this.status = PaymentStatus.COMPLETED
        this.approvedAt = LocalDateTime.now()
    }

    fun fail(reason: String) {
        this.status = PaymentStatus.FAILED
        this.failureReason = reason
    }

    fun cancel(reason: String) {
        this.status = PaymentStatus.CANCELLED
        this.cancelledAt = LocalDateTime.now()
        this.failureReason = reason
    }

    fun isPending(): Boolean = status == PaymentStatus.PENDING
    fun isCompleted(): Boolean = status == PaymentStatus.COMPLETED
    fun isFailed(): Boolean = status == PaymentStatus.FAILED
    fun isCancelled(): Boolean = status == PaymentStatus.CANCELLED
}

enum class PaymentMethod {
    ACCOUNT_TRANSFER,   // 계좌이체
    CARD,              // 카드
    VIRTUAL_ACCOUNT,   // 가상계좌
    MOBILE_PHONE,      // 휴대폰
    TOSS_PAY           // 토스페이
}

enum class PaymentStatus {
    PENDING,    // 결제 대기
    PROCESSING, // 결제 처리중
    COMPLETED,  // 결제 완료
    FAILED,     // 결제 실패
    CANCELLED,  // 결제 취소
    REFUNDED    // 환불
}