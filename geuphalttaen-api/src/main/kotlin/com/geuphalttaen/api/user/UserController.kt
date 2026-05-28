package com.geuphalttaen.api.user

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.user.MyReportResponse
import com.geuphalttaen.domain.user.UpdateNicknameRequest
import com.geuphalttaen.domain.user.UserProfileResponse
import com.geuphalttaen.domain.user.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@Validated
class UserController(
    private val userService: UserService,
) {
    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    fun getMyProfile(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<UserProfileResponse> =
        ApiResponse.ok(userService.getProfile(userId))

    @Operation(summary = "닉네임 수정")
    @PatchMapping("/me")
    fun updateNickname(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: UpdateNicknameRequest,
    ): ApiResponse<UserProfileResponse> =
        ApiResponse.ok(userService.updateNickname(userId, request))

    @Operation(summary = "회원 탈퇴")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAccount(
        @AuthenticationPrincipal userId: Long,
    ): Unit = userService.deleteAccount(userId)

    @Operation(summary = "내 제보 목록 조회")
    @GetMapping("/me/reports")
    fun getMyReports(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<List<MyReportResponse>> =
        ApiResponse.ok(userService.getMyReports(userId))
}
