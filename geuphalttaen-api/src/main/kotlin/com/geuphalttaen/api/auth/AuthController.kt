package com.geuphalttaen.api.auth

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.auth.AuthService
import com.geuphalttaen.domain.auth.OAuthLoginRequest
import com.geuphalttaen.domain.auth.TokenResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {
    @Operation(summary = "OAuth 로그인 (Kakao / Apple)")
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: OAuthLoginRequest,
    ): ApiResponse<TokenResponse> {
        val tokenResponse = authService.login(request)
        return ApiResponse.ok(tokenResponse)
    }
}
