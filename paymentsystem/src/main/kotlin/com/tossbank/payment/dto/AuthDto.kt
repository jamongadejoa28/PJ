package com.tossbank.payment.dto

import jakarta.validation.constraints.*

data class LoginRequest(
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @field:NotBlank(message = "이메일을 입력해주세요")
    val email: String,
    
    @field:NotBlank(message = "비밀번호를 입력해주세요")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    val password: String
)

data class SignUpRequest(
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @field:NotBlank(message = "이메일을 입력해주세요")
    val email: String,
    
    @field:NotBlank(message = "비밀번호를 입력해주세요")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    @field:Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]+$",
        message = "비밀번호는 영문자, 숫자, 특수문자를 포함해야 합니다"
    )
    val password: String,
    
    @field:NotBlank(message = "이름을 입력해주세요")
    @field:Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다")
    val name: String,
    
    @field:NotBlank(message = "전화번호를 입력해주세요")
    @field:Pattern(
        regexp = "^01[0-9]-[0-9]{4}-[0-9]{4}$",
        message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)"
    )
    val phoneNumber: String
)

data class JwtAuthenticationResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserSummary
)

data class UserSummary(
    val id: Long,
    val email: String,
    val name: String,
    val phoneNumber: String,
    val isEmailVerified: Boolean,
    val isPhoneVerified: Boolean
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class PasswordChangeRequest(
    @field:NotBlank(message = "현재 비밀번호를 입력해주세요")
    val currentPassword: String,
    
    @field:NotBlank(message = "새 비밀번호를 입력해주세요")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    @field:Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]+$",
        message = "비밀번호는 영문자, 숫자, 특수문자를 포함해야 합니다"
    )
    val newPassword: String
)