package com.geuphalttaen.api.admin

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.sync.SyncResultResponse
import com.geuphalttaen.domain.sync.ToiletSyncService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.Charset

@RestController
@RequestMapping("/api/v1/admin/toilets")
@Tag(name = "Admin", description = "관리자 API")
@SecurityRequirement(name = "bearerAuth")
@Validated
class AdminSyncController(
    private val toiletSyncService: ToiletSyncService,
) {
    @Operation(summary = "CSV 파일 업로드로 공공 화장실 데이터 동기화 (동기)")
    @PostMapping("/sync/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.OK)
    fun uploadAndSync(@RequestParam("file") file: MultipartFile): ApiResponse<SyncResultResponse> {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드 파일이 비어 있습니다.")
        }
        // 행정안전부 공공데이터 포털 CSV는 EUC-KR 인코딩으로 배포됨
        val syncLog = toiletSyncService.syncFromUpload(file.inputStream, Charset.forName("EUC-KR"))
        return ApiResponse.ok(
            SyncResultResponse(
                id = syncLog.id,
                status = syncLog.status.name,
                totalFetched = syncLog.totalFetched,
                insertedCount = syncLog.insertedCount,
                updatedCount = syncLog.updatedCount,
                deletedCount = syncLog.deletedCount,
                failedCount = syncLog.failedCount,
                syncedAt = syncLog.createdAt,
                errorMessage = syncLog.errorMessage,
            ),
        )
    }

    @Operation(summary = "동기화 이력 조회")
    @GetMapping("/sync/status")
    fun getSyncStatus(@RequestParam(defaultValue = "10") @Min(1) @Max(100) limit: Int): ApiResponse<List<SyncResultResponse>> =
        ApiResponse.ok(toiletSyncService.getSyncLogs(limit))
}
