package com.geuphalttaen.domain.admin

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * 관리자 로그인 요청 DTO.
 */
data class AdminLoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val password: String,
)

/**
 * 관리자 Access Token 응답 DTO.
 */
data class AdminTokenResponse(
    val accessToken: String,
)
