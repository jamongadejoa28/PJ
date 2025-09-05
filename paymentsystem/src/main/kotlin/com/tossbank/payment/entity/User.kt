package com.tossbank.payment.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_user_email", columnList = "email"),
        Index(name = "idx_user_phone", columnList = "phone_number")
    ]
)
class User(
    @Column(name = "email", nullable = false, unique = true, length = 100)
    var email: String,

    @Column(name = "password", nullable = false, length = 100)
    var password: String,

    @Column(name = "name", nullable = false, length = 50)
    var name: String,

    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    var phoneNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "failed_login_attempts", nullable = false)
    var failedLoginAttempts: Int = 0,

    @Column(name = "is_email_verified", nullable = false)
    var isEmailVerified: Boolean = false,

    @Column(name = "is_phone_verified", nullable = false)
    var isPhoneVerified: Boolean = false

) : BaseEntity() {

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var accounts: MutableList<Account> = mutableListOf()

    fun addAccount(account: Account) {
        accounts.add(account)
        account.user = this
    }

    fun incrementFailedLoginAttempts() {
        this.failedLoginAttempts++
        if (this.failedLoginAttempts >= 5) {
            this.status = UserStatus.LOCKED
        }
    }

    fun resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0
    }

    fun lock() {
        this.status = UserStatus.LOCKED
    }

    fun activate() {
        this.status = UserStatus.ACTIVE
        this.failedLoginAttempts = 0
    }
}

enum class UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED,
    WITHDRAWN
}