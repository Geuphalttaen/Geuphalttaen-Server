package com.geuphalttaen.domain.admin

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

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

/**
 * 최초 관리자 계정 생성 요청 DTO.
 */
data class AdminSeedRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 8, max = 72) val password: String,
)

/**
 * 제보 상태별 카운트 통계 응답 DTO.
 */
data class ReportStatsResponse(val pending: Int, val active: Int, val rejected: Int)
