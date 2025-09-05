package com.tossbank.payment.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean(name = ["paymentTaskExecutor"])
    fun paymentTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 20
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("payment-async-")
        executor.setRejectedExecutionHandler { _, _ ->
            // 큐가 가득 찰 경우 로깅만 하고 계속 진행 (결제 실패시키지 않음)
            println("Payment async task rejected - queue is full")
        }
        executor.initialize()
        return executor
    }

    @Bean(name = ["historyTaskExecutor"])
    fun historyTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 3
        executor.maxPoolSize = 10
        executor.queueCapacity = 500
        executor.setThreadNamePrefix("history-async-")
        executor.initialize()
        return executor
    }
}