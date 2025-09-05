package com.tossbank.payment.controller

import com.tossbank.payment.dto.*
import com.tossbank.payment.service.MerchantPaymentService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/merchant/payments")
class MerchantPaymentController {

    @Autowired
    private lateinit var merchantPaymentService: MerchantPaymentService

    @PostMapping("/request")
    fun requestPayment(
        @RequestHeader("Authorization") apiKey: String,
        @Valid @RequestBody request: MerchantPaymentRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<MerchantPaymentResponse>> {
        val response = merchantPaymentService.requestPayment(
            apiKey = apiKey.removePrefix("Bearer "),
            request = request,
            clientIp = getClientIpAddress(httpRequest)
        )
        
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/confirm")
    fun confirmPayment(
        @RequestHeader("Authorization") apiKey: String,
        @Valid @RequestBody request: PaymentCallbackRequest
    ): ResponseEntity<ApiResponse<PaymentResponse>> {
        val response = merchantPaymentService.confirmPayment(
            apiKey = apiKey.removePrefix("Bearer "),
            request = request
        )
        
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @GetMapping("/{transactionId}")
    fun getPaymentStatus(
        @RequestHeader("Authorization") apiKey: String,
        @PathVariable transactionId: String
    ): ResponseEntity<ApiResponse<PaymentResponse>> {
        val response = merchantPaymentService.getPaymentStatus(
            apiKey = apiKey.removePrefix("Bearer "),
            transactionId = transactionId
        )
        
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/{transactionId}/cancel")
    fun cancelPayment(
        @RequestHeader("Authorization") apiKey: String,
        @PathVariable transactionId: String,
        @Valid @RequestBody request: PaymentCancelRequest
    ): ResponseEntity<ApiResponse<String>> {
        val response = merchantPaymentService.cancelPayment(
            apiKey = apiKey.removePrefix("Bearer "),
            transactionId = transactionId,
            reason = request.reason
        )
        
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        val xRealIp = request.getHeader("X-Real-IP")

        return when {
            !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
            !xRealIp.isNullOrBlank() -> xRealIp
            else -> request.remoteAddr
        }
    }
}