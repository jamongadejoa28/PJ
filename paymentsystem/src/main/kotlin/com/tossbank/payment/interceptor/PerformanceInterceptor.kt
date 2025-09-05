package com.tossbank.payment.interceptor

import com.tossbank.payment.service.MonitoringService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

@Component
class PerformanceInterceptor : HandlerInterceptor {

    @Autowired
    private lateinit var monitoringService: MonitoringService

    companion object {
        private const val START_TIME_ATTRIBUTE = "startTime"
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val startTime = System.currentTimeMillis()
        request.setAttribute(START_TIME_ATTRIBUTE, startTime)
        
        // 요청 카운트 증가
        monitoringService.incrementRequestCount()
        
        return true
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        val startTime = request.getAttribute(START_TIME_ATTRIBUTE) as? Long ?: return
        val endTime = System.currentTimeMillis()
        val responseTime = endTime - startTime

        // 응답 시간 기록
        val endpoint = "${request.method} ${request.requestURI}"
        monitoringService.recordResponseTime(endpoint, responseTime)

        // 성공/실패 카운트 업데이트
        when (response.status) {
            in 200..299 -> monitoringService.incrementSuccessCount()
            in 400..599 -> monitoringService.incrementFailureCount()
        }

        // 응답 헤더에 처리 시간 추가
        response.setHeader("X-Response-Time", "${responseTime}ms")
        response.setHeader("X-Timestamp", System.currentTimeMillis().toString())
    }
}