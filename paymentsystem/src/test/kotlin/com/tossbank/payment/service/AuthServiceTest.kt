package com.tossbank.payment.service

import com.tossbank.payment.dto.LoginRequest
import com.tossbank.payment.dto.SignUpRequest
import com.tossbank.payment.entity.User
import com.tossbank.payment.entity.UserStatus
import com.tossbank.payment.repository.UserRepository
import com.tossbank.payment.security.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var authenticationManager: AuthenticationManager

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @InjectMocks
    private lateinit var authService: AuthService

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
        testUser.javaClass.getDeclaredField("id").apply {
            isAccessible = true
            set(testUser, 1L)
        }
    }

    @Test
    fun `should sign up user successfully`() {
        // Given
        val signUpRequest = SignUpRequest(
            email = "newuser@tossbank.com",
            password = "Password123!",
            name = "새 사용자",
            phoneNumber = "010-9876-5432"
        )

        `when`(userRepository.existsByEmail("newuser@tossbank.com")).thenReturn(false)
        `when`(userRepository.existsByPhoneNumber("010-9876-5432")).thenReturn(false)
        `when`(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword123!")
        `when`(userRepository.save(any<User>())).thenAnswer { invocation ->
            val user = invocation.getArgument<User>(0)
            user.javaClass.getDeclaredField("id").apply {
                isAccessible = true
                set(user, 2L)
            }
            user
        }

        // When
        val response = authService.signUp(signUpRequest)

        // Then
        assertThat(response.success).isTrue
        assertThat(response.message).isEqualTo("회원가입이 완료되었습니다.")
        assertThat(response.data?.email).isEqualTo("newuser@tossbank.com")
        assertThat(response.data?.name).isEqualTo("새 사용자")
        verify(userRepository).save(any<User>())
    }

    @Test
    fun `should not sign up user when email already exists`() {
        // Given
        val signUpRequest = SignUpRequest(
            email = "existing@tossbank.com",
            password = "Password123!",
            name = "새 사용자",
            phoneNumber = "010-9876-5432"
        )

        `when`(userRepository.existsByEmail("existing@tossbank.com")).thenReturn(true)

        // When
        val response = authService.signUp(signUpRequest)

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("이미 등록된 이메일입니다.")
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `should not sign up user when phone number already exists`() {
        // Given
        val signUpRequest = SignUpRequest(
            email = "newuser@tossbank.com",
            password = "Password123!",
            name = "새 사용자",
            phoneNumber = "010-1111-1111"
        )

        `when`(userRepository.existsByEmail("newuser@tossbank.com")).thenReturn(false)
        `when`(userRepository.existsByPhoneNumber("010-1111-1111")).thenReturn(true)

        // When
        val response = authService.signUp(signUpRequest)

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("이미 등록된 전화번호입니다.")
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `should sign in user successfully`() {
        // Given
        val loginRequest = LoginRequest(
            email = "test@tossbank.com",
            password = "password123"
        )

        val authentication = mock(Authentication::class.java)
        
        `when`(userRepository.findByEmail("test@tossbank.com")).thenReturn(testUser)
        `when`(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>())).thenReturn(authentication)
        `when`(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt-token")
        `when`(userRepository.save(testUser)).thenReturn(testUser)

        // When
        val response = authService.signIn(loginRequest)

        // Then
        assertThat(response.success).isTrue
        assertThat(response.message).isEqualTo("로그인 성공")
        assertThat(response.data?.accessToken).isEqualTo("jwt-token")
        assertThat(response.data?.user?.email).isEqualTo("test@tossbank.com")
        assertThat(testUser.failedLoginAttempts).isEqualTo(0)
        verify(userRepository).save(testUser)
    }

    @Test
    fun `should not sign in user when user not found`() {
        // Given
        val loginRequest = LoginRequest(
            email = "nonexistent@tossbank.com",
            password = "password123"
        )

        `when`(userRepository.findByEmail("nonexistent@tossbank.com")).thenReturn(null)

        // When
        val response = authService.signIn(loginRequest)

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("이메일 또는 비밀번호가 올바르지 않습니다.")
        verify(authenticationManager, never()).authenticate(any())
    }

    @Test
    fun `should not sign in locked user`() {
        // Given
        val loginRequest = LoginRequest(
            email = "test@tossbank.com",
            password = "password123"
        )

        testUser.status = UserStatus.LOCKED
        `when`(userRepository.findByEmail("test@tossbank.com")).thenReturn(testUser)

        // When
        val response = authService.signIn(loginRequest)

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("계정이 잠겨있습니다. 고객센터에 문의해주세요.")
        verify(authenticationManager, never()).authenticate(any())
    }

    @Test
    fun `should not sign in inactive user`() {
        // Given
        val loginRequest = LoginRequest(
            email = "test@tossbank.com",
            password = "password123"
        )

        testUser.status = UserStatus.INACTIVE
        `when`(userRepository.findByEmail("test@tossbank.com")).thenReturn(testUser)

        // When
        val response = authService.signIn(loginRequest)

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("비활성화된 계정입니다.")
        verify(authenticationManager, never()).authenticate(any())
    }

    @Test
    fun `should not sign in withdrawn user`() {
        // Given
        val loginRequest = LoginRequest(
            email = "test@tossbank.com",
            password = "password123"
        )

        testUser.status = UserStatus.WITHDRAWN
        `when`(userRepository.findByEmail("test@tossbank.com")).thenReturn(testUser)

        // When
        val response = authService.signIn(loginRequest)

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("탈퇴한 계정입니다.")
        verify(authenticationManager, never()).authenticate(any())
    }

    @Test
    fun `should increment failed login attempts on bad credentials`() {
        // Given
        val loginRequest = LoginRequest(
            email = "test@tossbank.com",
            password = "wrongpassword"
        )

        `when`(userRepository.findByEmail("test@tossbank.com")).thenReturn(testUser)
        `when`(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenThrow(BadCredentialsException::class.java)
        `when`(userRepository.save(testUser)).thenReturn(testUser)

        // When
        val response = authService.signIn(loginRequest)

        // Then
        assertThat(response.success).isFalse
        assertThat(response.message).isEqualTo("이메일 또는 비밀번호가 올바르지 않습니다.")
        assertThat(testUser.failedLoginAttempts).isEqualTo(1)
        verify(userRepository).save(testUser)
    }

    @Test
    fun `should lock user after 5 failed login attempts`() {
        // Given
        val loginRequest = LoginRequest(
            email = "test@tossbank.com",
            password = "wrongpassword"
        )

        testUser.failedLoginAttempts = 4 // Already has 4 failed attempts
        
        `when`(userRepository.findByEmail("test@tossbank.com")).thenReturn(testUser)
        `when`(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenThrow(BadCredentialsException::class.java)
        `when`(userRepository.save(testUser)).thenReturn(testUser)

        // When
        val response = authService.signIn(loginRequest)

        // Then
        assertThat(response.success).isFalse
        assertThat(testUser.failedLoginAttempts).isEqualTo(5)
        assertThat(testUser.status).isEqualTo(UserStatus.LOCKED)
        verify(userRepository).save(testUser)
    }
}