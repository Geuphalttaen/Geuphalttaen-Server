package com.geuphalttaen.api.admin

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.sync.SyncResultResponse
import com.geuphalttaen.domain.sync.ToiletSyncService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/toilets")
@Tag(name = "Admin", description = "관리자 API")
@SecurityRequirement(name = "bearerAuth")
class AdminSyncController(
    private val syncAsyncRunner: SyncAsyncRunner,
    private val toiletSyncService: ToiletSyncService,
) {
    @Operation(summary = "공공 화장실 데이터 수동 동기화 (비동기)")
    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun triggerSync(): ApiResponse<String> {
        syncAsyncRunner.runSyncAll()
        return ApiResponse.ok("동기화가 시작되었습니다.")
    }

    @Operation(summary = "동기화 이력 조회")
    @GetMapping("/sync/status")
    fun getSyncStatus(@RequestParam(defaultValue = "10") limit: Int): ApiResponse<List<SyncResultResponse>> =
        ApiResponse.ok(toiletSyncService.getSyncLogs(limit))
}
