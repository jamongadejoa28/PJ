package com.tossbank.payment.controller

import com.tossbank.payment.dto.*
import com.tossbank.payment.security.UserPrincipal
import com.tossbank.payment.service.AuthService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController {

    @Autowired
    private lateinit var authService: AuthService

    @PostMapping("/signup")
    fun signUp(@Valid @RequestBody signUpRequest: SignUpRequest): ResponseEntity<ApiResponse<UserSummary>> {
        val response = authService.signUp(signUpRequest)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/signin")
    fun signIn(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<ApiResponse<JwtAuthenticationResponse>> {
        val response = authService.signIn(loginRequest)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody passwordChangeRequest: PasswordChangeRequest
    ): ResponseEntity<ApiResponse<String>> {
        val response = authService.changePassword(userPrincipal.id, passwordChangeRequest)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<UserSummary>> {
        val userSummary = UserSummary(
            id = userPrincipal.id,
            email = userPrincipal.email,
            name = userPrincipal.name,
            phoneNumber = userPrincipal.username,
            isEmailVerified = true,
            isPhoneVerified = true
        )

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "사용자 정보 조회 성공",
                data = userSummary
            )
        )
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<ApiResponse<String>> {
        // JWT는 stateless하므로 서버에서 별도 로그아웃 처리 불필요
        // 클라이언트에서 토큰을 삭제하도록 안내
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "로그아웃되었습니다."
            )
        )
    }
}