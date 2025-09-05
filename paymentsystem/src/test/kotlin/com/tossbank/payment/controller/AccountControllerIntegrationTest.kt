package com.tossbank.payment.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.tossbank.payment.dto.CreateAccountRequest
import com.tossbank.payment.dto.DepositRequest
import com.tossbank.payment.entity.AccountType
import com.tossbank.payment.entity.User
import com.tossbank.payment.entity.UserStatus
import com.tossbank.payment.repository.UserRepository
import com.tossbank.payment.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class AccountControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var testUser: User
    private lateinit var jwtToken: String

    @BeforeEach
    fun setUp() {
        testUser = User(
            email = "test@tossbank.com",
            password = passwordEncoder.encode("password123"),
            name = "테스트 사용자",
            phoneNumber = "010-1234-5678",
            status = UserStatus.ACTIVE
        )
        testUser = userRepository.save(testUser)

        // Generate JWT token for authentication
        jwtToken = jwtTokenProvider.generateTokenFromUserId(testUser.id)
    }

    @Test
    fun `should create account successfully`() {
        // Given
        val createAccountRequest = CreateAccountRequest(
            accountName = "테스트 계좌",
            accountType = AccountType.CHECKING,
            dailyLimit = BigDecimal("3000000"),
            monthlyLimit = BigDecimal("30000000")
        )

        // When & Then
        mockMvc.perform(
            post("/api/accounts")
                .header("Authorization", "Bearer $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("계좌가 성공적으로 개설되었습니다."))
            .andExpect(jsonPath("$.data.accountName").value("테스트 계좌"))
            .andExpect(jsonPath("$.data.accountType").value("CHECKING"))
            .andExpect(jsonPath("$.data.balance").value(0))
            .andExpect(jsonPath("$.data.dailyLimit").value(3000000))
            .andExpect(jsonPath("$.data.monthlyLimit").value(30000000))
    }

    @Test
    fun `should fail to create account without authentication`() {
        // Given
        val createAccountRequest = CreateAccountRequest(
            accountName = "테스트 계좌",
            accountType = AccountType.CHECKING
        )

        // When & Then
        mockMvc.perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should fail to create account with invalid request`() {
        // Given - Empty account name
        val createAccountRequest = CreateAccountRequest(
            accountName = "",
            accountType = AccountType.CHECKING
        )

        // When & Then
        mockMvc.perform(
            post("/api/accounts")
                .header("Authorization", "Bearer $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should get user accounts successfully`() {
        // Given - Create a test account first
        val createAccountRequest = CreateAccountRequest(
            accountName = "테스트 계좌",
            accountType = AccountType.CHECKING
        )

        mockMvc.perform(
            post("/api/accounts")
                .header("Authorization", "Bearer $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest))
        )
            .andExpect(status().isOk)

        // When & Then
        mockMvc.perform(
            get("/api/accounts")
                .header("Authorization", "Bearer $jwtToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("계좌 목록 조회 성공"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].accountName").value("테스트 계좌"))
    }

    @Test
    fun `should deposit money successfully`() {
        // Given - Create a test account first
        val createAccountRequest = CreateAccountRequest(
            accountName = "테스트 계좌",
            accountType = AccountType.CHECKING
        )

        val createResult = mockMvc.perform(
            post("/api/accounts")
                .header("Authorization", "Bearer $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val response = objectMapper.readTree(createResult.response.contentAsString)
        val accountId = response.path("data").path("id").asLong()

        val depositRequest = DepositRequest(
            amount = BigDecimal("100000"),
            memo = "테스트 입금"
        )

        // When & Then
        mockMvc.perform(
            post("/api/accounts/$accountId/deposit")
                .header("Authorization", "Bearer $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("입금이 완료되었습니다."))
            .andExpect(jsonPath("$.data.transactionType").value("DEPOSIT"))
            .andExpect(jsonPath("$.data.amount").value(100000))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
    }

    @Test
    fun `should fail deposit with invalid amount`() {
        // Given - Create a test account first
        val createAccountRequest = CreateAccountRequest(
            accountName = "테스트 계좌",
            accountType = AccountType.CHECKING
        )

        val createResult = mockMvc.perform(
            post("/api/accounts")
                .header("Authorization", "Bearer $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val response = objectMapper.readTree(createResult.response.contentAsString)
        val accountId = response.path("data").path("id").asLong()

        val depositRequest = DepositRequest(
            amount = BigDecimal("500"), // Below minimum 1000
            memo = "테스트 입금"
        )

        // When & Then
        mockMvc.perform(
            post("/api/accounts/$accountId/deposit")
                .header("Authorization", "Bearer $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should get account balance successfully`() {
        // Given - Create a test account and make a deposit
        val createAccountRequest = CreateAccountRequest(
            accountName = "테스트 계좌",
            accountType = AccountType.CHECKING
        )

        val createResult = mockMvc.perform(
            post("/api/accounts")
                .header("Authorization", "Bearer $jwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val response = objectMapper.readTree(createResult.response.contentAsString)
        val accountId = response.path("data").path("id").asLong()

        // When & Then
        mockMvc.perform(
            get("/api/accounts/$accountId/balance")
                .header("Authorization", "Bearer $jwtToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("잔액 조회 성공"))
            .andExpect(jsonPath("$.data.balance").value(0))
            .andExpect(jsonPath("$.data.availableBalance").value(0))
    }
}