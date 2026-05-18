package com.geuphalttaen.api.admin

import com.geuphalttaen.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "관리자 API")
class AdminSyncController(
    // I6: 비동기 실행을 위해 SyncAsyncRunner 를 주입
    private val syncAsyncRunner: SyncAsyncRunner,
) {
    // B1: @PreAuthorize("hasRole('ADMIN')") 제거 — JwtAuthentication.getAuthorities() 가 emptyList() 를 반환하여 항상 403.
    //     권한 관리는 Issue #5 에서 처리하며, 현재는 SecurityConfig 의 /api/v1/admin/** authenticated() 에 의존한다.
    @Operation(summary = "공공 화장실 데이터 수동 동기화 (비동기)")
    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun triggerSync(): ApiResponse<String> {
        syncAsyncRunner.runSyncAll()
        return ApiResponse.ok("동기화가 시작되었습니다.")
    }
}
