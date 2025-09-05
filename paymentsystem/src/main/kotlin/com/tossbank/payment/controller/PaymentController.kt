package com.tossbank.payment.controller

import com.tossbank.payment.dto.*
import com.tossbank.payment.security.UserPrincipal
import com.tossbank.payment.service.PaymentService
import com.tossbank.payment.service.OptimizedPaymentService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/payments")
class PaymentController {

    @Autowired
    private lateinit var paymentService: PaymentService

    @Autowired
    private lateinit var optimizedPaymentService: OptimizedPaymentService

    @PostMapping
    fun processPayment(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: PaymentRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<PaymentResponse>> {
        // Add client info to request
        val enrichedRequest = request.copy(
            clientIp = getClientIpAddress(httpRequest),
            userAgent = httpRequest.getHeader("User-Agent")
        )
        
        val response = paymentService.processPayment(userPrincipal.id, enrichedRequest)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @GetMapping("/{transactionId}")
    fun getPayment(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable transactionId: String
    ): ResponseEntity<ApiResponse<PaymentResponse>> {
        val response = paymentService.getPayment(userPrincipal.id, transactionId)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @GetMapping
    fun getPayments(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<PaymentSummary>>> {
        val response = paymentService.getPayments(userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{transactionId}/cancel")
    fun cancelPayment(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable transactionId: String,
        @Valid @RequestBody request: PaymentCancelRequest
    ): ResponseEntity<ApiResponse<String>> {
        val response = paymentService.cancelPayment(userPrincipal.id, transactionId, request.reason)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    /**
     * 최적화된 결제 처리 엔드포인트
     * - 평균 응답시간 80% 개선 목표
     * - 비동기 처리를 통한 성능 향상
     */
    @PostMapping("/optimized")
    fun processPaymentOptimized(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: PaymentRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<PaymentResponse>> {
        val startTime = System.currentTimeMillis()
        
        // Add client info to request
        val enrichedRequest = request.copy(
            clientIp = getClientIpAddress(httpRequest),
            userAgent = httpRequest.getHeader("User-Agent")
        )

        val response = optimizedPaymentService.processPaymentOptimized(userPrincipal.id, enrichedRequest)
        
        val processingTime = System.currentTimeMillis() - startTime
        println("Optimized payment processing time: ${processingTime}ms")
        
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        val xRealIp = request.getHeader("X-Real-IP")
        val xForwardedProto = request.getHeader("X-Forwarded-Proto")

        return when {
            !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
            !xRealIp.isNullOrBlank() -> xRealIp
            else -> request.remoteAddr
        }
    }
}