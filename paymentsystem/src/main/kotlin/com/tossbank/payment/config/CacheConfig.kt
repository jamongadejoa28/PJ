package com.tossbank.payment.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10)) // 기본 TTL 10분
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues()

        val cacheConfigurations = mapOf(
            "users" to cacheConfiguration.entryTtl(Duration.ofMinutes(15)), // 사용자 정보: 15분
            "accounts" to cacheConfiguration.entryTtl(Duration.ofMinutes(5)), // 계좌 정보: 5분 (잔액 변동)
            "merchants" to cacheConfiguration.entryTtl(Duration.ofHours(1)), // 가맹점: 1시간 (자주 변경되지 않음)
            "paymentStats" to cacheConfiguration.entryTtl(Duration.ofMinutes(2)) // 통계: 2분
        )

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(cacheConfiguration)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}