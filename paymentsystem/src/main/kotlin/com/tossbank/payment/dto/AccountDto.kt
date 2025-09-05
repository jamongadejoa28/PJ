package com.tossbank.payment.dto

import com.tossbank.payment.entity.AccountStatus
import com.tossbank.payment.entity.AccountType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateAccountRequest(
    @field:NotBlank(message = "계좌명을 입력해주세요")
    @field:Size(min = 2, max = 50, message = "계좌명은 2자 이상 50자 이하여야 합니다")
    val accountName: String,
    
    @field:NotNull(message = "계좌 유형을 선택해주세요")
    val accountType: AccountType = AccountType.CHECKING,
    
    @field:NotNull(message = "일일 한도를 설정해주세요")
    @field:DecimalMin(value = "100000", message = "일일 한도는 최소 10만원 이상이어야 합니다")
    @field:DecimalMax(value = "10000000", message = "일일 한도는 최대 1천만원까지 가능합니다")
    val dailyLimit: BigDecimal = BigDecimal("5000000"),
    
    @field:NotNull(message = "월간 한도를 설정해주세요")
    @field:DecimalMin(value = "1000000", message = "월간 한도는 최소 100만원 이상이어야 합니다")
    @field:DecimalMax(value = "100000000", message = "월간 한도는 최대 1억원까지 가능합니다")
    val monthlyLimit: BigDecimal = BigDecimal("50000000")
)

data class DepositRequest(
    @field:NotNull(message = "입금 금액을 입력해주세요")
    @field:DecimalMin(value = "1000", message = "최소 입금액은 1,000원입니다")
    @field:DecimalMax(value = "50000000", message = "1회 최대 입금액은 5,000만원입니다")
    val amount: BigDecimal,
    
    @field:Size(max = 100, message = "입금 메모는 100자 이하여야 합니다")
    val memo: String? = null
)

data class WithdrawRequest(
    @field:NotNull(message = "출금 금액을 입력해주세요")
    @field:DecimalMin(value = "1000", message = "최소 출금액은 1,000원입니다")
    val amount: BigDecimal,
    
    @field:Size(max = 100, message = "출금 메모는 100자 이하여야 합니다")
    val memo: String? = null
)

data class TransferRequest(
    @field:NotBlank(message = "받는 계좌번호를 입력해주세요")
    val toAccountNumber: String,
    
    @field:NotNull(message = "송금 금액을 입력해주세요")
    @field:DecimalMin(value = "1000", message = "최소 송금액은 1,000원입니다")
    val amount: BigDecimal,
    
    @field:Size(max = 100, message = "송금 메모는 100자 이하여야 합니다")
    val memo: String? = null
)

data class AccountResponse(
    val id: Long,
    val accountNumber: String,
    val accountName: String,
    val balance: BigDecimal,
    val accountType: AccountType,
    val status: AccountStatus,
    val dailyLimit: BigDecimal,
    val monthlyLimit: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class AccountSummary(
    val id: Long,
    val accountNumber: String,
    val accountName: String,
    val balance: BigDecimal,
    val accountType: AccountType,
    val status: AccountStatus
)

data class TransactionRequest(
    @field:NotBlank(message = "거래 유형을 입력해주세요")
    val transactionType: String, // DEPOSIT, WITHDRAW, TRANSFER
    
    @field:NotNull(message = "거래 금액을 입력해주세요")
    @field:DecimalMin(value = "1", message = "거래 금액은 0보다 커야 합니다")
    val amount: BigDecimal,
    
    val targetAccountNumber: String? = null,
    
    @field:Size(max = 200, message = "거래 메모는 200자 이하여야 합니다")
    val memo: String? = null
)

data class TransactionResponse(
    val transactionId: String,
    val transactionType: String,
    val amount: BigDecimal,
    val fromAccountNumber: String,
    val toAccountNumber: String? = null,
    val memo: String? = null,
    val status: String,
    val createdAt: LocalDateTime
)

data class BalanceInquiryResponse(
    val accountNumber: String,
    val balance: BigDecimal,
    val availableBalance: BigDecimal,
    val dailyUsed: BigDecimal,
    val dailyLimit: BigDecimal,
    val monthlyUsed: BigDecimal,
    val monthlyLimit: BigDecimal,
    val inquiryTime: LocalDateTime = LocalDateTime.now()
)