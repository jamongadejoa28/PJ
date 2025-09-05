package com.tossbank.payment.repository

import com.tossbank.payment.entity.User
import com.tossbank.payment.entity.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    
    fun findByEmail(email: String): User?
    
    fun findByPhoneNumber(phoneNumber: String): User?
    
    fun existsByEmail(email: String): Boolean
    
    fun existsByPhoneNumber(phoneNumber: String): Boolean
    
    fun findByStatus(status: UserStatus): List<User>
    
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.deletedAt IS NULL")
    fun findActiveUsersByStatus(@Param("status") status: UserStatus): List<User>
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    fun findActiveUserByEmail(@Param("email") email: String): User?
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    fun countActiveUsers(): Long
}