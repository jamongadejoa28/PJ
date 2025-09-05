package com.tossbank.payment.repository

import com.tossbank.payment.entity.User
import com.tossbank.payment.entity.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        testUser = User(
            email = "test@tossbank.com",
            password = "encodedPassword123",
            name = "테스트 사용자",
            phoneNumber = "010-1234-5678",
            status = UserStatus.ACTIVE
        )
        entityManager.persistAndFlush(testUser)
    }

    @Test
    fun `should find user by email`() {
        // When
        val foundUser = userRepository.findByEmail("test@tossbank.com")

        // Then
        assertThat(foundUser).isNotNull
        assertThat(foundUser?.email).isEqualTo("test@tossbank.com")
        assertThat(foundUser?.name).isEqualTo("테스트 사용자")
    }

    @Test
    fun `should find user by phone number`() {
        // When
        val foundUser = userRepository.findByPhoneNumber("010-1234-5678")

        // Then
        assertThat(foundUser).isNotNull
        assertThat(foundUser?.phoneNumber).isEqualTo("010-1234-5678")
        assertThat(foundUser?.name).isEqualTo("테스트 사용자")
    }

    @Test
    fun `should return true when email exists`() {
        // When
        val exists = userRepository.existsByEmail("test@tossbank.com")

        // Then
        assertThat(exists).isTrue
    }

    @Test
    fun `should return false when email does not exist`() {
        // When
        val exists = userRepository.existsByEmail("nonexistent@tossbank.com")

        // Then
        assertThat(exists).isFalse
    }

    @Test
    fun `should find active user by email`() {
        // When
        val foundUser = userRepository.findActiveUserByEmail("test@tossbank.com")

        // Then
        assertThat(foundUser).isNotNull
        assertThat(foundUser?.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    fun `should not find inactive user by email`() {
        // Given
        testUser.status = UserStatus.INACTIVE
        entityManager.persistAndFlush(testUser)

        // When
        val foundUser = userRepository.findActiveUserByEmail("test@tossbank.com")

        // Then
        assertThat(foundUser).isNull()
    }

    @Test
    fun `should count active users correctly`() {
        // Given
        val inactiveUser = User(
            email = "inactive@tossbank.com",
            password = "encodedPassword456",
            name = "비활성 사용자",
            phoneNumber = "010-9876-5432",
            status = UserStatus.INACTIVE
        )
        entityManager.persistAndFlush(inactiveUser)

        // When
        val activeCount = userRepository.countActiveUsers()

        // Then
        assertThat(activeCount).isEqualTo(1L) // Only testUser is active
    }

    @Test
    fun `should find users by status`() {
        // Given
        val lockedUser = User(
            email = "locked@tossbank.com",
            password = "encodedPassword789",
            name = "잠긴 사용자",
            phoneNumber = "010-5555-5555",
            status = UserStatus.LOCKED
        )
        entityManager.persistAndFlush(lockedUser)

        // When
        val activeUsers = userRepository.findByStatus(UserStatus.ACTIVE)
        val lockedUsers = userRepository.findByStatus(UserStatus.LOCKED)

        // Then
        assertThat(activeUsers).hasSize(1)
        assertThat(activeUsers[0].email).isEqualTo("test@tossbank.com")
        
        assertThat(lockedUsers).hasSize(1)
        assertThat(lockedUsers[0].email).isEqualTo("locked@tossbank.com")
    }
}