package com.tossbank.payment.repository

import com.tossbank.payment.entity.Merchant
import com.tossbank.payment.entity.MerchantCategory
import com.tossbank.payment.entity.MerchantStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MerchantRepository : JpaRepository<Merchant, Long> {
    
    fun findByMerchantCode(merchantCode: String): Merchant?
    
    fun findByApiKey(apiKey: String): Merchant?
    
    fun findByBusinessNumber(businessNumber: String): Merchant?
    
    fun findByStatus(status: MerchantStatus): List<Merchant>
    
    fun findByCategory(category: MerchantCategory): List<Merchant>
    
    fun existsByMerchantCode(merchantCode: String): Boolean
    
    fun existsByBusinessNumber(businessNumber: String): Boolean
    
    fun existsByApiKey(apiKey: String): Boolean
    
    @Query("SELECT m FROM Merchant m WHERE m.status = 'ACTIVE' AND m.deletedAt IS NULL")
    fun findActiveMerchants(): List<Merchant>
    
    @Query("SELECT m FROM Merchant m WHERE m.merchantCode = :merchantCode AND m.status = 'ACTIVE' AND m.deletedAt IS NULL")
    fun findActiveMerchantByCode(@Param("merchantCode") merchantCode: String): Merchant?
    
    @Query("SELECT m FROM Merchant m WHERE m.apiKey = :apiKey AND m.status = 'ACTIVE' AND m.deletedAt IS NULL")
    fun findActiveMerchantByApiKey(@Param("apiKey") apiKey: String): Merchant?
    
    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.status = 'ACTIVE' AND m.deletedAt IS NULL")
    fun countActiveMerchants(): Long
}