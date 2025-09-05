package com.tossbank.payment.config

import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.*
import org.springframework.kafka.config.TopicBuilder

@Configuration
@EnableKafka
class KafkaConfig {

    companion object {
        const val BOOTSTRAP_SERVERS = "localhost:9092"
    }

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        val configs = mapOf(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to BOOTSTRAP_SERVERS
        )
        return KafkaAdmin(configs)
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to BOOTSTRAP_SERVERS,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            // 성능 최적화 설정
            ProducerConfig.ACKS_CONFIG to "1", // 리더만 확인
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.BATCH_SIZE_CONFIG to 16384,
            ProducerConfig.LINGER_MS_CONFIG to 5,
            ProducerConfig.BUFFER_MEMORY_CONFIG to 33554432
        )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory())
    }

    // 토픽 자동 생성
    @Bean
    fun paymentTopic(): NewTopic {
        return TopicBuilder.name("tossbank.payment.events")
            .partitions(3)  // 3개 파티션으로 확장성 확보
            .replicas(1)    // 개발환경이므로 1개 복제본
            .build()
    }

    @Bean
    fun notificationTopic(): NewTopic {
        return TopicBuilder.name("tossbank.notification.events")
            .partitions(2)  
            .replicas(1)
            .build()
    }

    @Bean
    fun auditTopic(): NewTopic {
        return TopicBuilder.name("tossbank.audit.events")
            .partitions(1)
            .replicas(1)
            .build()
    }
}