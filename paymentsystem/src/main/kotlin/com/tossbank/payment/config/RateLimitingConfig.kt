package com.tossbank.payment.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Configuration
class RateLimitingConfig {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    @Bean
    fun rateLimitBuckets(): ConcurrentHashMap<String, Bucket> {
        return buckets
    }

    // 사용자별 Rate Limiting: 분당 100회
    fun getUserBucket(userId: String): Bucket {
        return buckets.computeIfAbsent("user:$userId") {
            createUserBucket()
        }
    }

    // 가맹점별 Rate Limiting: 분당 1000회  
    fun getMerchantBucket(apiKey: String): Bucket {
        return buckets.computeIfAbsent("merchant:$apiKey") {
            createMerchantBucket()
        }
    }

    // IP별 Rate Limiting: 분당 200회
    fun getIpBucket(ip: String): Bucket {
        return buckets.computeIfAbsent("ip:$ip") {
            createIpBucket()
        }
    }

    private fun createUserBucket(): Bucket {
        val limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)))
        return Bucket.builder()
            .addLimit(limit)
            .build()
    }

    private fun createMerchantBucket(): Bucket {
        val limit = Bandwidth.classic(1000, Refill.intervally(1000, Duration.ofMinutes(1)))
        return Bucket.builder()
            .addLimit(limit)
            .build()
    }

    private fun createIpBucket(): Bucket {
        val limit = Bandwidth.classic(200, Refill.intervally(200, Duration.ofMinutes(1)))
        return Bucket.builder()
            .addLimit(limit)
            .build()
    }
}