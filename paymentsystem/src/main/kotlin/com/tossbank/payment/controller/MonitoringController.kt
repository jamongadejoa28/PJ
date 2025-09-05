package com.tossbank.payment.controller

import com.tossbank.payment.service.MonitoringService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/monitoring")
class MonitoringController {

    @Autowired
    private lateinit var monitoringService: MonitoringService

    @GetMapping("/stats")
    fun getRealTimeStats(): ResponseEntity<Map<String, Any>> {
        val stats = monitoringService.getRealTimeStats()
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/health")
    fun getSystemHealth(): ResponseEntity<Map<String, Any>> {
        val health = monitoringService.getSystemHealth()
        return ResponseEntity.ok(health)
    }

    @GetMapping("/alerts")
    fun getAlerts(): ResponseEntity<List<Map<String, Any>>> {
        val alerts = monitoringService.checkAlerts()
        return ResponseEntity.ok(alerts)
    }

    @GetMapping("/dashboard")
    fun getDashboardData(): ResponseEntity<Map<String, Any>> {
        val stats = monitoringService.getRealTimeStats()
        val health = monitoringService.getSystemHealth()
        val alerts = monitoringService.checkAlerts()

        val dashboard = mapOf(
            "stats" to stats,
            "health" to health,
            "alerts" to alerts,
            "timestamp" to System.currentTimeMillis()
        )

        return ResponseEntity.ok(dashboard)
    }
}