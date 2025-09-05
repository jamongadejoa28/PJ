package com.tossbank.payment.service

import com.tossbank.payment.dto.*
import com.tossbank.payment.entity.Account
import com.tossbank.payment.entity.AccountStatus
import com.tossbank.payment.entity.User
import com.tossbank.payment.repository.AccountRepository
import com.tossbank.payment.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
@Transactional
class AccountService {

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private val secureRandom = SecureRandom()

    fun createAccount(userId: Long, request: CreateAccountRequest): ApiResponse<AccountResponse> {
        val user = userRepository.findById(userId).orElse(null)
            ?: return ApiResponse(
                success = false,
                message = "사용자를 찾을 수 없습니다."
            )

        // Check if user already has 5 accounts (limit)
        val existingAccounts = accountRepository.findActiveAccountsByUserId(userId)
        if (existingAccounts.size >= 5) {
            return ApiResponse(
                success = false,
                message = "계좌는 최대 5개까지 개설 가능합니다."
            )
        }

        // Generate unique account number
        val accountNumber = generateAccountNumber()

        val account = Account(
            accountNumber = accountNumber,
            accountName = request.accountName,
            balance = BigDecimal.ZERO,
            accountType = request.accountType,
            status = AccountStatus.ACTIVE,
            dailyLimit = request.dailyLimit,
            monthlyLimit = request.monthlyLimit
        )
        
        account.user = user
        val savedAccount = accountRepository.save(account)

        val accountResponse = AccountResponse(
            id = savedAccount.id,
            accountNumber = savedAccount.accountNumber,
            accountName = savedAccount.accountName,
            balance = savedAccount.balance,
            accountType = savedAccount.accountType,
            status = savedAccount.status,
            dailyLimit = savedAccount.dailyLimit,
            monthlyLimit = savedAccount.monthlyLimit,
            createdAt = savedAccount.createdAt,
            updatedAt = savedAccount.updatedAt
        )

        return ApiResponse(
            success = true,
            message = "계좌가 성공적으로 개설되었습니다.",
            data = accountResponse
        )
    }

    @Transactional(readOnly = true)
    fun getAccounts(userId: Long): ApiResponse<List<AccountSummary>> {
        val accounts = accountRepository.findActiveAccountsByUserId(userId)
        
        val accountSummaries = accounts.map { account ->
            AccountSummary(
                id = account.id,
                accountNumber = account.accountNumber,
                accountName = account.accountName,
                balance = account.balance,
                accountType = account.accountType,
                status = account.status
            )
        }

        return ApiResponse(
            success = true,
            message = "계좌 목록 조회 성공",
            data = accountSummaries
        )
    }

    @Transactional(readOnly = true)
    fun getAccount(userId: Long, accountId: Long): ApiResponse<AccountResponse> {
        val account = accountRepository.findById(accountId).orElse(null)
            ?: return ApiResponse(
                success = false,
                message = "계좌를 찾을 수 없습니다."
            )

        // Check if the account belongs to the user
        if (account.user.id != userId) {
            return ApiResponse(
                success = false,
                message = "계좌에 접근할 권한이 없습니다."
            )
        }

        val accountResponse = AccountResponse(
            id = account.id,
            accountNumber = account.accountNumber,
            accountName = account.accountName,
            balance = account.balance,
            accountType = account.accountType,
            status = account.status,
            dailyLimit = account.dailyLimit,
            monthlyLimit = account.monthlyLimit,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt
        )

        return ApiResponse(
            success = true,
            message = "계좌 상세 조회 성공",
            data = accountResponse
        )
    }

    fun deposit(userId: Long, accountId: Long, request: DepositRequest): ApiResponse<TransactionResponse> {
        val account = accountRepository.findById(accountId).orElse(null)
            ?: return ApiResponse(
                success = false,
                message = "계좌를 찾을 수 없습니다."
            )

        // Check if the account belongs to the user
        if (account.user.id != userId) {
            return ApiResponse(
                success = false,
                message = "계좌에 접근할 권한이 없습니다."
            )
        }

        // Check account status
        if (account.status != AccountStatus.ACTIVE) {
            return ApiResponse(
                success = false,
                message = "계좌가 활성 상태가 아닙니다."
            )
        }

        // Deposit money
        account.deposit(request.amount)
        accountRepository.save(account)

        val transactionResponse = TransactionResponse(
            transactionId = generateTransactionId(),
            transactionType = "DEPOSIT",
            amount = request.amount,
            fromAccountNumber = account.accountNumber,
            memo = request.memo,
            status = "COMPLETED",
            createdAt = LocalDateTime.now()
        )

        return ApiResponse(
            success = true,
            message = "입금이 완료되었습니다.",
            data = transactionResponse
        )
    }

    fun withdraw(userId: Long, accountId: Long, request: WithdrawRequest): ApiResponse<TransactionResponse> {
        val account = accountRepository.findById(accountId).orElse(null)
            ?: return ApiResponse(
                success = false,
                message = "계좌를 찾을 수 없습니다."
            )

        // Check if the account belongs to the user
        if (account.user.id != userId) {
            return ApiResponse(
                success = false,
                message = "계좌에 접근할 권한이 없습니다."
            )
        }

        // Check account status
        if (account.status != AccountStatus.ACTIVE) {
            return ApiResponse(
                success = false,
                message = "계좌가 활성 상태가 아닙니다."
            )
        }

        // Attempt withdrawal
        if (!account.withdraw(request.amount)) {
            return ApiResponse(
                success = false,
                message = "잔액이 부족하거나 일일 한도를 초과했습니다."
            )
        }

        accountRepository.save(account)

        val transactionResponse = TransactionResponse(
            transactionId = generateTransactionId(),
            transactionType = "WITHDRAW",
            amount = request.amount,
            fromAccountNumber = account.accountNumber,
            memo = request.memo,
            status = "COMPLETED",
            createdAt = LocalDateTime.now()
        )

        return ApiResponse(
            success = true,
            message = "출금이 완료되었습니다.",
            data = transactionResponse
        )
    }

    fun transfer(userId: Long, accountId: Long, request: TransferRequest): ApiResponse<TransactionResponse> {
        val fromAccount = accountRepository.findById(accountId).orElse(null)
            ?: return ApiResponse(
                success = false,
                message = "송금할 계좌를 찾을 수 없습니다."
            )

        // Check if the account belongs to the user
        if (fromAccount.user.id != userId) {
            return ApiResponse(
                success = false,
                message = "계좌에 접근할 권한이 없습니다."
            )
        }

        val toAccount = accountRepository.findActiveAccountByAccountNumber(request.toAccountNumber)
            ?: return ApiResponse(
                success = false,
                message = "받는 계좌를 찾을 수 없습니다."
            )

        // Check if trying to transfer to same account
        if (fromAccount.id == toAccount.id) {
            return ApiResponse(
                success = false,
                message = "같은 계좌로는 송금할 수 없습니다."
            )
        }

        // Check account statuses
        if (fromAccount.status != AccountStatus.ACTIVE || toAccount.status != AccountStatus.ACTIVE) {
            return ApiResponse(
                success = false,
                message = "계좌가 활성 상태가 아닙니다."
            )
        }

        // Perform transfer
        if (!fromAccount.withdraw(request.amount)) {
            return ApiResponse(
                success = false,
                message = "잔액이 부족하거나 일일 한도를 초과했습니다."
            )
        }

        toAccount.deposit(request.amount)
        
        accountRepository.save(fromAccount)
        accountRepository.save(toAccount)

        val transactionResponse = TransactionResponse(
            transactionId = generateTransactionId(),
            transactionType = "TRANSFER",
            amount = request.amount,
            fromAccountNumber = fromAccount.accountNumber,
            toAccountNumber = toAccount.accountNumber,
            memo = request.memo,
            status = "COMPLETED",
            createdAt = LocalDateTime.now()
        )

        return ApiResponse(
            success = true,
            message = "송금이 완료되었습니다.",
            data = transactionResponse
        )
    }

    @Transactional(readOnly = true)
    fun getBalance(userId: Long, accountId: Long): ApiResponse<BalanceInquiryResponse> {
        val account = accountRepository.findById(accountId).orElse(null)
            ?: return ApiResponse(
                success = false,
                message = "계좌를 찾을 수 없습니다."
            )

        // Check if the account belongs to the user
        if (account.user.id != userId) {
            return ApiResponse(
                success = false,
                message = "계좌에 접근할 권한이 없습니다."
            )
        }

        val balanceResponse = BalanceInquiryResponse(
            accountNumber = account.accountNumber,
            balance = account.balance,
            availableBalance = account.balance, // 단순화: 실제로는 보류 금액 등을 고려
            dailyUsed = BigDecimal.ZERO, // TODO: 실제 일일 사용량 계산
            dailyLimit = account.dailyLimit,
            monthlyUsed = BigDecimal.ZERO, // TODO: 실제 월간 사용량 계산
            monthlyLimit = account.monthlyLimit
        )

        return ApiResponse(
            success = true,
            message = "잔액 조회 성공",
            data = balanceResponse
        )
    }

    private fun generateAccountNumber(): String {
        val prefix = "100" // 토스뱅크 계좌 번호 prefix
        var accountNumber: String
        
        do {
            val randomPart = String.format("%08d", secureRandom.nextInt(100000000))
            accountNumber = "$prefix$randomPart"
        } while (accountRepository.existsByAccountNumber(accountNumber))
        
        return accountNumber
    }

    private fun generateTransactionId(): String {
        val timestamp = System.currentTimeMillis()
        val random = secureRandom.nextInt(10000)
        return "TXN$timestamp$random"
    }
}