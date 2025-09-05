package com.tossbank.payment.service

import com.tossbank.payment.dto.*
import com.tossbank.payment.entity.User
import com.tossbank.payment.entity.UserStatus
import com.tossbank.payment.repository.UserRepository
import com.tossbank.payment.security.JwtTokenProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthService {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var authenticationManager: AuthenticationManager

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    fun signUp(signUpRequest: SignUpRequest): ApiResponse<UserSummary> {
        // Check if email already exists
        if (userRepository.existsByEmail(signUpRequest.email)) {
            return ApiResponse(
                success = false,
                message = "이미 등록된 이메일입니다."
            )
        }

        // Check if phone number already exists
        if (userRepository.existsByPhoneNumber(signUpRequest.phoneNumber)) {
            return ApiResponse(
                success = false,
                message = "이미 등록된 전화번호입니다."
            )
        }

        // Create new user
        val user = User(
            email = signUpRequest.email,
            password = passwordEncoder.encode(signUpRequest.password),
            name = signUpRequest.name,
            phoneNumber = signUpRequest.phoneNumber,
            status = UserStatus.ACTIVE
        )

        val savedUser = userRepository.save(user)

        val userSummary = UserSummary(
            id = savedUser.id,
            email = savedUser.email,
            name = savedUser.name,
            phoneNumber = savedUser.phoneNumber,
            isEmailVerified = savedUser.isEmailVerified,
            isPhoneVerified = savedUser.isPhoneVerified
        )

        return ApiResponse(
            success = true,
            message = "회원가입이 완료되었습니다.",
            data = userSummary
        )
    }

    fun signIn(loginRequest: LoginRequest): ApiResponse<JwtAuthenticationResponse> {
        try {
            // Find user by email
            val user = userRepository.findByEmail(loginRequest.email)
                ?: return ApiResponse(
                    success = false,
                    message = "이메일 또는 비밀번호가 올바르지 않습니다."
                )

            // Check user status
            if (user.status != UserStatus.ACTIVE) {
                return ApiResponse(
                    success = false,
                    message = when (user.status) {
                        UserStatus.LOCKED -> "계정이 잠겨있습니다. 고객센터에 문의해주세요."
                        UserStatus.INACTIVE -> "비활성화된 계정입니다."
                        UserStatus.WITHDRAWN -> "탈퇴한 계정입니다."
                        else -> "사용할 수 없는 계정입니다."
                    }
                )
            }

            // Authenticate user
            val authentication: Authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)
            )

            // Generate JWT token using user ID
            val jwt = jwtTokenProvider.generateTokenFromUserId(user.id)
            val expiresIn = 86400000L // 24 hours

            // Reset failed login attempts on successful login
            user.resetFailedLoginAttempts()
            userRepository.save(user)

            val userSummary = UserSummary(
                id = user.id,
                email = user.email,
                name = user.name,
                phoneNumber = user.phoneNumber,
                isEmailVerified = user.isEmailVerified,
                isPhoneVerified = user.isPhoneVerified
            )

            val authResponse = JwtAuthenticationResponse(
                accessToken = jwt,
                expiresIn = expiresIn,
                user = userSummary
            )

            return ApiResponse(
                success = true,
                message = "로그인 성공",
                data = authResponse
            )

        } catch (ex: BadCredentialsException) {
            // Handle failed login attempts
            val user = userRepository.findByEmail(loginRequest.email)
            user?.let {
                it.incrementFailedLoginAttempts()
                userRepository.save(it)
            }

            return ApiResponse(
                success = false,
                message = "이메일 또는 비밀번호가 올바르지 않습니다."
            )
        } catch (ex: Exception) {
            println("로그인 오류 발생: ${ex.message}")
            ex.printStackTrace()
            return ApiResponse(
                success = false,
                message = "로그인 처리 중 오류가 발생했습니다: ${ex.message}"
            )
        }
    }

    fun changePassword(userId: Long, passwordChangeRequest: PasswordChangeRequest): ApiResponse<String> {
        val user = userRepository.findById(userId).orElse(null)
            ?: return ApiResponse(
                success = false,
                message = "사용자를 찾을 수 없습니다."
            )

        // Verify current password
        if (!passwordEncoder.matches(passwordChangeRequest.currentPassword, user.password)) {
            return ApiResponse(
                success = false,
                message = "현재 비밀번호가 올바르지 않습니다."
            )
        }

        // Update password
        user.password = passwordEncoder.encode(passwordChangeRequest.newPassword)
        userRepository.save(user)

        return ApiResponse(
            success = true,
            message = "비밀번호가 변경되었습니다."
        )
    }
}