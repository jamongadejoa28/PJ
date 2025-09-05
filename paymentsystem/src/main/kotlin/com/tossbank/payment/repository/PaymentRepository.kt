package com.tossbank.payment.repository

import com.tossbank.payment.entity.Payment
import com.tossbank.payment.entity.PaymentMethod
import com.tossbank.payment.entity.PaymentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {
    
    fun findByTransactionId(transactionId: String): Payment?
    
    fun findByMerchantOrderId(merchantOrderId: String): Payment?
    
    fun findByUserId(userId: Long, pageable: Pageable): Page<Payment>
    
    fun findByMerchantId(merchantId: Long, pageable: Pageable): Page<Payment>
    
    fun findByStatus(status: PaymentStatus): List<Payment>
    
    fun findByPaymentMethod(paymentMethod: PaymentMethod): List<Payment>
    
    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.status = :status ORDER BY p.createdAt DESC")
    fun findByUserIdAndStatus(@Param("userId") userId: Long, @Param("status") status: PaymentStatus, pageable: Pageable): Page<Payment>
    
    @Query("SELECT p FROM Payment p WHERE p.merchant.id = :merchantId AND p.status = :status ORDER BY p.createdAt DESC")
    fun findByMerchantIdAndStatus(@Param("merchantId") merchantId: Long, @Param("status") status: PaymentStatus, pageable: Pageable): Page<Payment>
    
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    fun findByDateRange(@Param("startDate") startDate: LocalDateTime, @Param("endDate") endDate: LocalDateTime, pageable: Pageable): Page<Payment>
    
    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    fun findByUserIdAndDateRange(@Param("userId") userId: Long, @Param("startDate") startDate: LocalDateTime, @Param("endDate") endDate: LocalDateTime, pageable: Pageable): Page<Payment>
    
    @Query("SELECT p FROM Payment p WHERE p.merchant.id = :merchantId AND p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    fun findByMerchantIdAndDateRange(@Param("merchantId") merchantId: Long, @Param("startDate") startDate: LocalDateTime, @Param("endDate") endDate: LocalDateTime, pageable: Pageable): Page<Payment>
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    fun getTotalAmountByDateRange(@Param("startDate") startDate: LocalDateTime, @Param("endDate") endDate: LocalDateTime): BigDecimal?
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.merchant.id = :merchantId AND p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    fun getTotalAmountByMerchantAndDateRange(@Param("merchantId") merchantId: Long, @Param("startDate") startDate: LocalDateTime, @Param("endDate") endDate: LocalDateTime): BigDecimal?
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.createdAt >= :date")
    fun countByStatusSince(@Param("status") status: PaymentStatus, @Param("date") date: LocalDateTime): Long
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :timeoutDate")
    fun findTimeoutPayments(@Param("timeoutDate") timeoutDate: LocalDateTime): List<Payment>
    
    // 사용자별 결제 내역 조회 (최신순)
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Payment>
}