package com.geuphalttaen.api.admin

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.sync.SyncStatusResponse
import com.geuphalttaen.domain.sync.ToiletSyncService
import com.geuphalttaen.infra.sync.PublicToiletSyncJob
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/toilets/sync")
class AdminSyncController(
    private val syncJob: PublicToiletSyncJob,
    private val toiletSyncService: ToiletSyncService,
) {
    @PostMapping
    fun triggerSync(): ApiResponse<String> {
        syncJob.syncWeekly()
        return ApiResponse.ok("동기화가 시작되었습니다.")
    }

    @GetMapping("/status")
    fun getSyncStatus(): ApiResponse<SyncStatusResponse?> {
        return ApiResponse.ok(toiletSyncService.getLastSyncStatus())
    }
}
