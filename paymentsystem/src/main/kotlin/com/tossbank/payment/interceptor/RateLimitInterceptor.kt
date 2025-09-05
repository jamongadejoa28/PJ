package com.tossbank.payment.interceptor

import com.tossbank.payment.config.RateLimitingConfig
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RateLimitInterceptor : HandlerInterceptor {

    @Autowired
    private lateinit var rateLimitingConfig: RateLimitingConfig

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val clientIp = getClientIpAddress(request)
        val uri = request.requestURI

        // Rate Limiting 적용 대상 확인
        when {
            uri.startsWith("/api/payments") -> {
                // 사용자 결제 API: JWT에서 사용자 ID 추출하여 제한
                val userId = extractUserIdFromJwt(request)
                if (userId != null) {
                    val userBucket = rateLimitingConfig.getUserBucket(userId)
                    if (!userBucket.tryConsume(1)) {
                        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                        response.writer.write(
                            """{"success":false,"message":"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.","errorCode":"RATE_LIMIT_EXCEEDED"}"""
                        )
                        return false
                    }
                }
            }
            uri.startsWith("/api/merchant/payments") -> {
                // 가맹점 API: API 키 기반 제한
                val apiKey = request.getHeader("Authorization")?.removePrefix("Bearer ")
                if (apiKey != null) {
                    val merchantBucket = rateLimitingConfig.getMerchantBucket(apiKey)
                    if (!merchantBucket.tryConsume(1)) {
                        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                        response.writer.write(
                            """{"success":false,"message":"API 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요.","errorCode":"MERCHANT_RATE_LIMIT_EXCEEDED"}"""
                        )
                        return false
                    }
                }
            }
            uri.startsWith("/api/auth") -> {
                // 인증 API: IP 기반 제한 (더 엄격)
                val ipBucket = rateLimitingConfig.getIpBucket(clientIp)
                if (!ipBucket.tryConsume(1)) {
                    response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                    response.writer.write(
                        """{"success":false,"message":"인증 요청이 너무 많습니다. 15분 후 다시 시도해주세요.","errorCode":"AUTH_RATE_LIMIT_EXCEEDED"}"""
                    )
                    return false
                }
            }
        }

        // IP 기반 전역 Rate Limiting
        val globalIpBucket = rateLimitingConfig.getIpBucket("global:$clientIp")
        if (!globalIpBucket.tryConsume(1)) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.writer.write(
                """{"success":false,"message":"요청 빈도가 너무 높습니다. 잠시 후 다시 시도해주세요.","errorCode":"GLOBAL_RATE_LIMIT_EXCEEDED"}"""
            )
            return false
        }

        return true
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

    private fun extractUserIdFromJwt(request: HttpServletRequest): String? {
        // JWT 토큰에서 사용자 ID 추출 (간단한 구현)
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                val token = authHeader.substring(7)
                // JWT 파싱해서 사용자 ID 추출
                // 실제 구현에서는 JwtTokenProvider를 사용해야 함
                return "user_from_jwt"
            } catch (e: Exception) {
                // 토큰이 유효하지 않은 경우
                return null
            }
        }
        return null
    }
}