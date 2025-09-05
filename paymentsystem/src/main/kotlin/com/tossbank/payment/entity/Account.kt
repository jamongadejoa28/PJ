package com.tossbank.payment.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "accounts",
    indexes = [
        Index(name = "idx_account_number", columnList = "account_number"),
        Index(name = "idx_account_user", columnList = "user_id")
    ]
)
class Account(
    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    var accountNumber: String,

    @Column(name = "account_name", nullable = false, length = 50)
    var accountName: String,

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    var balance: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    var accountType: AccountType = AccountType.CHECKING,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: AccountStatus = AccountStatus.ACTIVE,

    @Column(name = "daily_limit", nullable = false, precision = 15, scale = 2)
    var dailyLimit: BigDecimal = BigDecimal("5000000"), // 500만원

    @Column(name = "monthly_limit", nullable = false, precision = 15, scale = 2)
    var monthlyLimit: BigDecimal = BigDecimal("50000000") // 5천만원

) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User

    fun withdraw(amount: BigDecimal): Boolean {
        if (canWithdraw(amount)) {
            balance = balance.subtract(amount)
            return true
        }
        return false
    }

    fun deposit(amount: BigDecimal) {
        balance = balance.add(amount)
    }

    private fun canWithdraw(amount: BigDecimal): Boolean {
        return balance >= amount && 
               status == AccountStatus.ACTIVE && 
               amount <= dailyLimit
    }

    fun freeze() {
        this.status = AccountStatus.FROZEN
    }

    fun activate() {
        this.status = AccountStatus.ACTIVE
    }

    fun close() {
        this.status = AccountStatus.CLOSED
    }
}

enum class AccountType {
    CHECKING,    // 입출금
    SAVINGS,     // 적금
    DEPOSIT      // 예금
}

enum class AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED,
    DORMANT
}