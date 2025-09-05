package com.tossbank.payment.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "merchants",
    indexes = [
        Index(name = "idx_merchant_code", columnList = "merchant_code"),
        Index(name = "idx_merchant_business_number", columnList = "business_number")
    ]
)
class Merchant(
    @Column(name = "merchant_code", nullable = false, unique = true, length = 20)
    var merchantCode: String,

    @Column(name = "merchant_name", nullable = false, length = 100)
    var merchantName: String,

    @Column(name = "business_number", nullable = false, unique = true, length = 12)
    var businessNumber: String,

    @Column(name = "representative_name", nullable = false, length = 50)
    var representativeName: String,

    @Column(name = "phone_number", nullable = false, length = 15)
    var phoneNumber: String,

    @Column(name = "email", nullable = false, length = 100)
    var email: String,

    @Column(name = "address", nullable = false, length = 200)
    var address: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    var category: MerchantCategory = MerchantCategory.GENERAL,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: MerchantStatus = MerchantStatus.ACTIVE,

    @Column(name = "api_key", nullable = false, length = 64)
    var apiKey: String,

    @Column(name = "webhook_url", length = 200)
    var webhookUrl: String? = null

) : BaseEntity() {

    @OneToMany(mappedBy = "merchant", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var payments: MutableList<Payment> = mutableListOf()

    fun suspend() {
        this.status = MerchantStatus.SUSPENDED
    }

    fun activate() {
        this.status = MerchantStatus.ACTIVE
    }

    fun terminate() {
        this.status = MerchantStatus.TERMINATED
    }
}

enum class MerchantCategory {
    GENERAL,        // 일반
    RESTAURANT,     // 음식점
    RETAIL,         // 소매
    ONLINE,         // 온라인쇼핑몰
    EDUCATION,      // 교육
    HEALTHCARE,     // 의료
    ENTERTAINMENT,  // 엔터테인먼트
    TRANSPORT       // 교통
}

enum class MerchantStatus {
    PENDING,    // 승인 대기
    ACTIVE,     // 활성
    SUSPENDED,  // 정지
    TERMINATED  // 해지
}