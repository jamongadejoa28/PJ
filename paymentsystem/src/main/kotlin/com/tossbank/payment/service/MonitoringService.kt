package com.tossbank.payment.service

import com.tossbank.payment.repository.PaymentRepository
import com.tossbank.payment.entity.PaymentStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap

@Service
class MonitoringService {

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    // 실시간 메트릭스
    private val requestCount = AtomicLong(0)
    private val successCount = AtomicLong(0)
    private val failureCount = AtomicLong(0)
    private val responseTimeMap = ConcurrentHashMap<String, MutableList<Long>>()

    // API 호출 카운트 증가
    fun incrementRequestCount() {
        requestCount.incrementAndGet()
    }

    fun incrementSuccessCount() {
        successCount.incrementAndGet()
    }

    fun incrementFailureCount() {
        failureCount.incrementAndGet()
    }

    // 응답 시간 기록
    fun recordResponseTime(endpoint: String, responseTime: Long) {
        responseTimeMap.computeIfAbsent(endpoint) { mutableListOf() }.add(responseTime)
        
        // 최근 100개만 유지
        val times = responseTimeMap[endpoint]!!
        if (times.size > 100) {
            times.removeAt(0)
        }
    }

    // 실시간 통계 조회
    fun getRealTimeStats(): Map<String, Any> {
        val now = LocalDateTime.now()
        val oneHourAgo = now.minusHours(1)
        val oneDayAgo = now.minusDays(1)

        return mapOf(
            "current_time" to now.toString(),
            "total_requests" to requestCount.get(),
            "success_requests" to successCount.get(),
            "failure_requests" to failureCount.get(),
            "success_rate" to if (requestCount.get() > 0) {
                (successCount.get().toDouble() / requestCount.get() * 100).toString() + "%"
            } else "0%",
            "hourly_payments" to paymentRepository.countByStatusSince(PaymentStatus.COMPLETED, oneHourAgo),
            "daily_payments" to paymentRepository.countByStatusSince(PaymentStatus.COMPLETED, oneDayAgo),
            "hourly_revenue" to (paymentRepository.getTotalAmountByDateRange(oneHourAgo, now) ?: BigDecimal.ZERO),
            "daily_revenue" to (paymentRepository.getTotalAmountByDateRange(oneDayAgo, now) ?: BigDecimal.ZERO),
            "avg_response_times" to getAverageResponseTimes()
        )
    }

    private fun getAverageResponseTimes(): Map<String, Double> {
        return responseTimeMap.mapValues { (_, times) ->
            if (times.isNotEmpty()) {
                times.filterNotNull().takeIf { it.isNotEmpty() }?.average() ?: 0.0
            } else 0.0
        }
    }

    // 시스템 헬스 체크
    fun getSystemHealth(): Map<String, Any> {
        val stats = getRealTimeStats()
        val successRate = extractSuccessRate(stats["success_rate"].toString())
        val avgResponseTime = getOverallAverageResponseTime()

        val healthStatus = when {
            successRate >= 99.9 && avgResponseTime <= 200 -> "EXCELLENT"
            successRate >= 99.5 && avgResponseTime <= 500 -> "GOOD"  
            successRate >= 99.0 && avgResponseTime <= 1000 -> "FAIR"
            successRate >= 95.0 && avgResponseTime <= 2000 -> "POOR"
            else -> "CRITICAL"
        }

        return mapOf(
            "status" to healthStatus,
            "success_rate" to successRate,
            "avg_response_time_ms" to avgResponseTime,
            "uptime" to getUptime(),
            "memory_usage" to getMemoryUsage(),
            "active_connections" to getActiveConnections()
        )
    }

    private fun extractSuccessRate(successRateStr: String): Double {
        return try {
            successRateStr.replace("%", "").toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    private fun getOverallAverageResponseTime(): Double {
        val allTimes = responseTimeMap.values.flatten().filterNotNull()
        return if (allTimes.isNotEmpty()) {
            allTimes.average()
        } else 0.0
    }

    private fun getUptime(): String {
        // 간단한 업타임 계산 (실제로는 애플리케이션 시작 시간을 기록해야 함)
        return "System uptime information"
    }

    private fun getMemoryUsage(): Map<String, Long> {
        val runtime = Runtime.getRuntime()
        return mapOf(
            "total_memory" to runtime.totalMemory(),
            "free_memory" to runtime.freeMemory(),
            "used_memory" to (runtime.totalMemory() - runtime.freeMemory()),
            "max_memory" to runtime.maxMemory()
        )
    }

    private fun getActiveConnections(): Int {
        // 실제로는 데이터베이스 연결 풀에서 정보를 가져와야 함
        return 0
    }

    // 알럼 체크
    fun checkAlerts(): List<Map<String, Any>> {
        val alerts = mutableListOf<Map<String, Any>>()
        val health = getSystemHealth()
        val successRate = health["success_rate"] as Double
        val avgResponseTime = health["avg_response_time_ms"] as Double

        if (successRate < 95.0) {
            alerts.add(mapOf(
                "type" to "ERROR",
                "message" to "성공률이 95% 미만입니다: ${successRate}%",
                "severity" to "HIGH"
            ))
        }

        if (avgResponseTime > 1000) {
            alerts.add(mapOf(
                "type" to "WARNING", 
                "message" to "평균 응답시간이 1초를 초과했습니다: ${avgResponseTime}ms",
                "severity" to "MEDIUM"
            ))
        }

        val failureRate = failureCount.get().toDouble() / (requestCount.get().coerceAtLeast(1)) * 100
        if (failureRate > 5.0) {
            alerts.add(mapOf(
                "type" to "WARNING",
                "message" to "실패율이 5%를 초과했습니다: ${failureRate}%", 
                "severity" to "MEDIUM"
            ))
        }

        return alerts
    }
}