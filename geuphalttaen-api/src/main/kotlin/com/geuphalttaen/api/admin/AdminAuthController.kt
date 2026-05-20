package com.geuphalttaen.api.admin

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.admin.AdminLoginRequest
import com.geuphalttaen.domain.admin.AdminSeedRequest
import com.geuphalttaen.domain.admin.AdminService
import com.geuphalttaen.domain.admin.AdminTokenResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 관리자 인증 컨트롤러.
 */
@Tag(name = "Admin Auth", description = "관리자 인증 API")
@RestController
@RequestMapping("/api/v1/admin/auth")
class AdminAuthController(
    private val adminService: AdminService,
) {

    @Operation(summary = "관리자 로그인 (이메일/패스워드)")
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: AdminLoginRequest,
    ): ApiResponse<AdminTokenResponse> {
        val token = adminService.login(request)
        return ApiResponse.ok(token)
    }

    @Operation(
        summary = "최초 관리자 계정 생성 (관리자가 없을 때만 허용)",
        description = "관리자 계정이 한 명도 없을 때 최초 계정을 생성한다. " +
            "X-Seed-Secret 헤더에 환경변수로 설정된 시크릿 값을 전달해야 한다. " +
            "이미 관리자가 존재하면 409 Conflict를 반환한다.",
    )
    @PostMapping("/seed")
    @ResponseStatus(HttpStatus.CREATED)
    fun seed(
        @RequestHeader("X-Seed-Secret") seedSecret: String,
        @Valid @RequestBody request: AdminSeedRequest,
    ): ApiResponse<Unit> {
        adminService.seedAdmin(seedSecret, request)
        return ApiResponse.ok()
    }
}
