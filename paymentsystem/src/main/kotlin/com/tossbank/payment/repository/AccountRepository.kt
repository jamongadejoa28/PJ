package com.tossbank.payment.repository

import com.tossbank.payment.entity.Account
import com.tossbank.payment.entity.AccountStatus
import com.tossbank.payment.entity.AccountType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface AccountRepository : JpaRepository<Account, Long> {
    
    fun findByAccountNumber(accountNumber: String): Account?
    
    fun findByUserId(userId: Long): List<Account>
    
    fun findByUserIdAndStatus(userId: Long, status: AccountStatus): List<Account>
    
    fun findByUserIdAndAccountType(userId: Long, accountType: AccountType): List<Account>
    
    fun existsByAccountNumber(accountNumber: String): Boolean
    
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.status = 'ACTIVE' AND a.deletedAt IS NULL")
    fun findActiveAccountsByUserId(@Param("userId") userId: Long): List<Account>
    
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber AND a.status = 'ACTIVE' AND a.deletedAt IS NULL")
    fun findActiveAccountByAccountNumber(@Param("accountNumber") accountNumber: String): Account?
    
    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.user.id = :userId AND a.status = 'ACTIVE' AND a.deletedAt IS NULL")
    fun getTotalBalanceByUserId(@Param("userId") userId: Long): BigDecimal?
    
    @Query("SELECT a FROM Account a WHERE a.balance >= :minBalance AND a.status = 'ACTIVE' AND a.deletedAt IS NULL")
    fun findAccountsWithMinBalance(@Param("minBalance") minBalance: BigDecimal): List<Account>
}