package com.geuphalttaen.api.admin

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.admin.AdminLoginRequest
import com.geuphalttaen.domain.admin.AdminService
import com.geuphalttaen.domain.admin.AdminTokenResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
}
