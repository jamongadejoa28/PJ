package com.tossbank.payment.service

import com.tossbank.payment.dto.CreateAccountRequest
import com.tossbank.payment.dto.DepositRequest
import com.tossbank.payment.dto.WithdrawRequest
import com.tossbank.payment.dto.TransferRequest
import com.tossbank.payment.entity.Account
import com.tossbank.payment.entity.AccountStatus
import com.tossbank.payment.entity.AccountType
import com.tossbank.payment.entity.User
import com.tossbank.payment.entity.UserStatus
import com.tossbank.payment.repository.AccountRepository
import com.tossbank.payment.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.util.*

@ExtendWith(MockitoExtension::class)
class AccountServiceTest {

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var accountService: AccountService

    private lateinit var testUser: User
    private lateinit var testAccount: Account

    @BeforeEach
    fun setUp() {
        testUser = User(
            email = "test@tossbank.com",
            password = "encodedPassword123",
            name = "테스트 사용자",
            phoneNumber = "010-1234-5678",
            status = UserStatus.ACTIVE
        )
        testUser.javaClass.getDeclaredField("id").apply {
            isAccessible = true
            set(testUser, 1L)
        }

        testAccount = Account(
            accountNumber = "100123456789",
            accountName = "테스트 계좌",
            balance = BigDecimal("100000"),
            accountType = AccountType.CHECKING,
            status = AccountStatus.ACTIVE,
            dailyLimit = BigDecimal("5000000"),
            monthlyLimit = BigDecimal("50000000")
        )
        testAccount.user = testUser
        testAccount.javaClass.getDeclaredField("id").apply {
            isAccessible = true
            set(testAccount, 1L)
        }
    }

    @Test
    fun `should create account successfully`() {
        // Given
        val request = CreateAccountRequest(
            accountName = "새 계좌",
            accountType = AccountType.CHECKING,
            dailyLimit = BigDecimal("3000000"),
            monthlyLimit = BigDecimal("30000000")
        )

        `when`(userRepository.findById(1L)).thenReturn(Optional.of(testUser))
        `when`(accountRepository.findActiveAccountsByUserId(1L)).thenReturn(emptyList())
        `when`(accountRepository.existsByAccountNumber(any())).thenReturn(false)
        `when`(accountRepository.save(any<Account>())).thenAnswer { invocation ->
            val account = invocation.getArgument<Account>(0)
            account.javaClass.getDeclaredField("id").apply {
                isAccessible = true
                set(account, 2L)
            }
            account
        }

        // When
        val response = accountService.createAccount(1L, request)

        // Then
        assertThat(response.success).isTrue
        assertThat(response.data?.accountName).isEqualTo("새 계좌")
        assertThat(response.data?.accountType).isEqualTo(AccountType.CHECKING)
        verify(accountRepository).save(any<Account>())
    }

    @Test
    fun `should not create account when user not found`() {
        // Given
        val request = CreateAccountRequest(
            accountName = "새 계좌",
            accountType = AccountType.CHECKING
        )

        `when`(userRepository.findById(1L)).thenReturn(Optional.empty())

        // When
        val response = accountService.createAccount(1L, request)

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("사용자를 찾을 수 없습니다.")
        verify(accountRepository, never()).save(any<Account>())
    }

    @Test
    fun `should not create account when user has 5 accounts already`() {
        // Given
        val request = CreateAccountRequest(
            accountName = "새 계좌",
            accountType = AccountType.CHECKING
        )

        val existingAccounts = List(5) { testAccount }

        `when`(userRepository.findById(1L)).thenReturn(Optional.of(testUser))
        `when`(accountRepository.findActiveAccountsByUserId(1L)).thenReturn(existingAccounts)

        // When
        val response = accountService.createAccount(1L, request)

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("계좌는 최대 5개까지 개설 가능합니다.")
        verify(accountRepository, never()).save(any<Account>())
    }

    @Test
    fun `should deposit money successfully`() {
        // Given
        val depositRequest = DepositRequest(
            amount = BigDecimal("50000"),
            memo = "입금 테스트"
        )

        `when`(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount))
        `when`(accountRepository.save(testAccount)).thenReturn(testAccount)

        // When
        val response = accountService.deposit(1L, 1L, depositRequest)

        // Then
        assertThat(response.success).isTrue
        assertThat(response.data?.transactionType).isEqualTo("DEPOSIT")
        assertThat(response.data?.amount).isEqualTo(BigDecimal("50000"))
        assertThat(testAccount.balance).isEqualTo(BigDecimal("150000")) // 100000 + 50000
        verify(accountRepository).save(testAccount)
    }

    @Test
    fun `should withdraw money successfully`() {
        // Given
        val withdrawRequest = WithdrawRequest(
            amount = BigDecimal("30000"),
            memo = "출금 테스트"
        )

        `when`(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount))
        `when`(accountRepository.save(testAccount)).thenReturn(testAccount)

        // When
        val response = accountService.withdraw(1L, 1L, withdrawRequest)

        // Then
        assertThat(response.success).isTrue
        assertThat(response.data?.transactionType).isEqualTo("WITHDRAW")
        assertThat(response.data?.amount).isEqualTo(BigDecimal("30000"))
        assertThat(testAccount.balance).isEqualTo(BigDecimal("70000")) // 100000 - 30000
        verify(accountRepository).save(testAccount)
    }

    @Test
    fun `should not withdraw when insufficient balance`() {
        // Given
        val withdrawRequest = WithdrawRequest(
            amount = BigDecimal("200000"), // More than balance
            memo = "출금 테스트"
        )

        `when`(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount))

        // When
        val response = accountService.withdraw(1L, 1L, withdrawRequest)

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("잔액이 부족하거나 일일 한도를 초과했습니다.")
        verify(accountRepository, never()).save(any<Account>())
    }

    @Test
    fun `should transfer money successfully`() {
        // Given
        val toAccount = Account(
            accountNumber = "100987654321",
            accountName = "받는 계좌",
            balance = BigDecimal("50000"),
            accountType = AccountType.CHECKING,
            status = AccountStatus.ACTIVE,
            dailyLimit = BigDecimal("5000000"),
            monthlyLimit = BigDecimal("50000000")
        )
        toAccount.user = testUser
        toAccount.javaClass.getDeclaredField("id").apply {
            isAccessible = true
            set(toAccount, 2L)
        }

        val transferRequest = TransferRequest(
            toAccountNumber = "100987654321",
            amount = BigDecimal("30000"),
            memo = "송금 테스트"
        )

        `when`(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount))
        `when`(accountRepository.findActiveAccountByAccountNumber("100987654321")).thenReturn(toAccount)
        `when`(accountRepository.save(any<Account>())).thenAnswer { it.getArgument(0) }

        // When
        val response = accountService.transfer(1L, 1L, transferRequest)

        // Then
        assertThat(response.success).isTrue
        assertThat(response.data?.transactionType).isEqualTo("TRANSFER")
        assertThat(response.data?.amount).isEqualTo(BigDecimal("30000"))
        assertThat(response.data?.fromAccountNumber).isEqualTo("100123456789")
        assertThat(response.data?.toAccountNumber).isEqualTo("100987654321")
        
        assertThat(testAccount.balance).isEqualTo(BigDecimal("70000")) // 100000 - 30000
        assertThat(toAccount.balance).isEqualTo(BigDecimal("80000"))   // 50000 + 30000
        
        verify(accountRepository, times(2)).save(any<Account>())
    }

    @Test
    fun `should not transfer to same account`() {
        // Given
        val transferRequest = TransferRequest(
            toAccountNumber = "100123456789", // Same as fromAccount
            amount = BigDecimal("30000"),
            memo = "송금 테스트"
        )

        `when`(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount))
        `when`(accountRepository.findActiveAccountByAccountNumber("100123456789")).thenReturn(testAccount)

        // When
        val response = accountService.transfer(1L, 1L, transferRequest)

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("같은 계좌로는 송금할 수 없습니다.")
        verify(accountRepository, never()).save(any<Account>())
    }

    @Test
    fun `should not transfer when target account not found`() {
        // Given
        val transferRequest = TransferRequest(
            toAccountNumber = "100999999999",
            amount = BigDecimal("30000"),
            memo = "송금 테스트"
        )

        `when`(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount))
        `when`(accountRepository.findActiveAccountByAccountNumber("100999999999")).thenReturn(null)

        // When
        val response = accountService.transfer(1L, 1L, transferRequest)

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("받는 계좌를 찾을 수 없습니다.")
        verify(accountRepository, never()).save(any<Account>())
    }

    @Test
    fun `should get account balance successfully`() {
        // Given
        `when`(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount))

        // When
        val response = accountService.getBalance(1L, 1L)

        // Then
        assertThat(response.success).isTrue
        assertThat(response.data?.balance).isEqualTo(BigDecimal("100000"))
        assertThat(response.data?.accountNumber).isEqualTo("100123456789")
        assertThat(response.data?.dailyLimit).isEqualTo(BigDecimal("5000000"))
    }

    @Test
    fun `should not access account of different user`() {
        // Given
        val otherUser = User(
            email = "other@tossbank.com",
            password = "encodedPassword456",
            name = "다른 사용자",
            phoneNumber = "010-9876-5432",
            status = UserStatus.ACTIVE
        )
        otherUser.javaClass.getDeclaredField("id").apply {
            isAccessible = true
            set(otherUser, 2L)
        }

        val otherAccount = Account(
            accountNumber = "100111111111",
            accountName = "다른 계좌",
            balance = BigDecimal("200000"),
            accountType = AccountType.CHECKING,
            status = AccountStatus.ACTIVE,
            dailyLimit = BigDecimal("5000000"),
            monthlyLimit = BigDecimal("50000000")
        )
        otherAccount.user = otherUser
        otherAccount.javaClass.getDeclaredField("id").apply {
            isAccessible = true
            set(otherAccount, 2L)
        }

        `when`(accountRepository.findById(2L)).thenReturn(Optional.of(otherAccount))

        // When
        val response = accountService.getBalance(1L, 2L) // User 1 trying to access account 2

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("계좌에 접근할 권한이 없습니다.")
    }
}