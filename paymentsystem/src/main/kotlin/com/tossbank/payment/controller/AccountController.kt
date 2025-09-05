package com.tossbank.payment.controller

import com.tossbank.payment.dto.*
import com.tossbank.payment.security.UserPrincipal
import com.tossbank.payment.service.AccountService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/accounts")
class AccountController {

    @Autowired
    private lateinit var accountService: AccountService

    @PostMapping
    fun createAccount(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: CreateAccountRequest
    ): ResponseEntity<ApiResponse<AccountResponse>> {
        val response = accountService.createAccount(userPrincipal.id, request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @GetMapping
    fun getAccounts(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<AccountSummary>>> {
        val response = accountService.getAccounts(userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{accountId}")
    fun getAccount(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable accountId: Long
    ): ResponseEntity<ApiResponse<AccountResponse>> {
        val response = accountService.getAccount(userPrincipal.id, accountId)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @GetMapping("/{accountId}/balance")
    fun getBalance(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable accountId: Long
    ): ResponseEntity<ApiResponse<BalanceInquiryResponse>> {
        val response = accountService.getBalance(userPrincipal.id, accountId)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/{accountId}/deposit")
    fun deposit(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable accountId: Long,
        @Valid @RequestBody request: DepositRequest
    ): ResponseEntity<ApiResponse<TransactionResponse>> {
        val response = accountService.deposit(userPrincipal.id, accountId, request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/{accountId}/withdraw")
    fun withdraw(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable accountId: Long,
        @Valid @RequestBody request: WithdrawRequest
    ): ResponseEntity<ApiResponse<TransactionResponse>> {
        val response = accountService.withdraw(userPrincipal.id, accountId, request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/{accountId}/transfer")
    fun transfer(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable accountId: Long,
        @Valid @RequestBody request: TransferRequest
    ): ResponseEntity<ApiResponse<TransactionResponse>> {
        val response = accountService.transfer(userPrincipal.id, accountId, request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/{accountId}/transaction")
    fun processTransaction(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable accountId: Long,
        @Valid @RequestBody request: TransactionRequest
    ): ResponseEntity<ApiResponse<TransactionResponse>> {
        val response = when (request.transactionType.uppercase()) {
            "DEPOSIT" -> {
                val depositRequest = DepositRequest(request.amount, request.memo)
                accountService.deposit(userPrincipal.id, accountId, depositRequest)
            }
            "WITHDRAW" -> {
                val withdrawRequest = WithdrawRequest(request.amount, request.memo)
                accountService.withdraw(userPrincipal.id, accountId, withdrawRequest)
            }
            "TRANSFER" -> {
                if (request.targetAccountNumber.isNullOrBlank()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse<TransactionResponse>(
                            success = false,
                            message = "송금할 계좌번호를 입력해주세요."
                        )
                    )
                }
                val transferRequest = TransferRequest(request.targetAccountNumber, request.amount, request.memo)
                accountService.transfer(userPrincipal.id, accountId, transferRequest)
            }
            else -> {
                ApiResponse<TransactionResponse>(
                    success = false,
                    message = "지원하지 않는 거래 유형입니다."
                )
            }
        }

        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }
}