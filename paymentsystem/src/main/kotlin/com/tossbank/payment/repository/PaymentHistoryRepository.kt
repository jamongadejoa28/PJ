package com.tossbank.payment.repository

import com.tossbank.payment.entity.PaymentHistory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PaymentHistoryRepository : JpaRepository<PaymentHistory, Long> {
    
    fun findByPaymentId(paymentId: Long): List<PaymentHistory>
    
    fun findByPaymentIdOrderByCreatedAtDesc(paymentId: Long): List<PaymentHistory>
    
    fun findByProcessedBy(processedBy: String, pageable: Pageable): Page<PaymentHistory>
    
    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.payment.transactionId = :transactionId ORDER BY ph.createdAt DESC")
    fun findByTransactionId(@Param("transactionId") transactionId: String): List<PaymentHistory>
    
    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.createdAt BETWEEN :startDate AND :endDate ORDER BY ph.createdAt DESC")
    fun findByDateRange(@Param("startDate") startDate: LocalDateTime, @Param("endDate") endDate: LocalDateTime, pageable: Pageable): Page<PaymentHistory>
    
    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.payment.id = :paymentId AND ph.createdAt BETWEEN :startDate AND :endDate ORDER BY ph.createdAt DESC")
    fun findByPaymentIdAndDateRange(@Param("paymentId") paymentId: Long, @Param("startDate") startDate: LocalDateTime, @Param("endDate") endDate: LocalDateTime): List<PaymentHistory>
    
    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.currentStatus = :status AND ph.createdAt BETWEEN :startDate AND :endDate ORDER BY ph.createdAt DESC")
    fun findByStatusAndDateRange(@Param("status") status: String, @Param("startDate") startDate: LocalDateTime, @Param("endDate") endDate: LocalDateTime, pageable: Pageable): Page<PaymentHistory>
    
    @Query("SELECT COUNT(ph) FROM PaymentHistory ph WHERE ph.currentStatus = :status AND ph.createdAt >= :date")
    fun countByStatusSince(@Param("status") status: String, @Param("date") date: LocalDateTime): Long
}