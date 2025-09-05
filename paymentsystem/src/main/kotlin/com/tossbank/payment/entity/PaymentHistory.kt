package com.tossbank.payment.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "payment_histories",
    indexes = [
        Index(name = "idx_payment_history_payment", columnList = "payment_id"),
        Index(name = "idx_payment_history_created_at", columnList = "created_at")
    ]
)
class PaymentHistory(
    @Column(name = "previous_status", length = 20)
    var previousStatus: String? = null,

    @Column(name = "current_status", nullable = false, length = 20)
    var currentStatus: String,

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    var amount: BigDecimal,

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Column(name = "processed_by", length = 50)
    var processedBy: String? = null,

    @Column(name = "client_ip", length = 45)
    var clientIp: String? = null,

    @Column(name = "additional_info", columnDefinition = "TEXT")
    var additionalInfo: String? = null

) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    lateinit var payment: Payment

    companion object {
        fun createStatusChangeHistory(
            payment: Payment,
            previousStatus: String?,
            currentStatus: String,
            description: String? = null,
            processedBy: String? = null,
            clientIp: String? = null
        ): PaymentHistory {
            return PaymentHistory(
                previousStatus = previousStatus,
                currentStatus = currentStatus,
                amount = payment.amount,
                description = description,
                processedBy = processedBy,
                clientIp = clientIp
            ).apply {
                this.payment = payment
            }
        }
    }
}