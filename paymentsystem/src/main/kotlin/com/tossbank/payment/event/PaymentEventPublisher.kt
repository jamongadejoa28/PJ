package com.tossbank.payment.event

import com.tossbank.payment.entity.Payment
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.scheduling.annotation.Async
import java.time.LocalDateTime

data class PaymentEvent(
    val transactionId: String,
    val userId: Long,
    val amount: java.math.BigDecimal,
    val paymentMethod: String,
    val status: String,
    val merchantCode: String,
    val timestamp: LocalDateTime,
    val eventType: String
)

@Service
class PaymentEventPublisher {

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        const val PAYMENT_TOPIC = "tossbank.payment.events"
        const val NOTIFICATION_TOPIC = "tossbank.notification.events"
    }

    /**
     * 결제 완료 이벤트 발행
     */
    @Async
    fun publishPaymentCompletedEvent(payment: Payment) {
        try {
            val event = PaymentEvent(
                transactionId = payment.transactionId,
                userId = payment.user.id,
                amount = payment.amount,
                paymentMethod = payment.paymentMethod.name,
                status = payment.status.name,
                merchantCode = payment.merchant.merchantCode,
                timestamp = LocalDateTime.now(),
                eventType = "PAYMENT_COMPLETED"
            )

            val eventJson = objectMapper.writeValueAsString(event)
            kafkaTemplate.send(PAYMENT_TOPIC, payment.transactionId, eventJson)
            
            println(" Payment event published: ${payment.transactionId}")
        } catch (ex: Exception) {
            println(" Failed to publish payment event: ${ex.message}")
        }
    }

    /**
     * 결제 실패 이벤트 발행
     */
    @Async
    fun publishPaymentFailedEvent(payment: Payment) {
        try {
            val event = PaymentEvent(
                transactionId = payment.transactionId,
                userId = payment.user.id,
                amount = payment.amount,
                paymentMethod = payment.paymentMethod.name,
                status = payment.status.name,
                merchantCode = payment.merchant.merchantCode,
                timestamp = LocalDateTime.now(),
                eventType = "PAYMENT_FAILED"
            )

            val eventJson = objectMapper.writeValueAsString(event)
            kafkaTemplate.send(PAYMENT_TOPIC, payment.transactionId, eventJson)
            
            println(" Payment failed event published: ${payment.transactionId}")
        } catch (ex: Exception) {
            println(" Failed to publish payment failed event: ${ex.message}")
        }
    }

    /**
     * 알림 이벤트 발행 (이메일, SMS, 푸시)
     */
    @Async
    fun publishNotificationEvent(transactionId: String, userId: Long, message: String, type: String) {
        try {
            val notificationEvent = mapOf(
                "transactionId" to transactionId,
                "userId" to userId,
                "message" to message,
                "type" to type,  // EMAIL, SMS, PUSH
                "timestamp" to LocalDateTime.now().toString()
            )

            val eventJson = objectMapper.writeValueAsString(notificationEvent)
            kafkaTemplate.send(NOTIFICATION_TOPIC, userId.toString(), eventJson)
            
            println("📧 Notification event published: $type for user $userId")
        } catch (ex: Exception) {
            println(" Failed to publish notification event: ${ex.message}")
        }
    }
}