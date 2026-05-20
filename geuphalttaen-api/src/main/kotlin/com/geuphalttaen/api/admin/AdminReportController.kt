package com.geuphalttaen.api.admin

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.admin.AdminService
import com.geuphalttaen.domain.admin.ReportStatsResponse
import com.geuphalttaen.domain.toilet.AdminToiletResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 관리자 제보 관리 컨트롤러.
 */
@Tag(name = "Admin Reports", description = "관리자 제보 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/reports")
@Validated
class AdminReportController(
    private val adminService: AdminService,
) {

    @Operation(summary = "제보 통계 조회")
    @GetMapping("/stats")
    fun getReportStats(): ApiResponse<ReportStatsResponse> =
        ApiResponse.ok(adminService.getReportStats())

    @Operation(summary = "제보 목록 조회 (페이징, status 필터 옵션)")
    @GetMapping
    fun getReports(
        @RequestParam(required = false) status: ToiletStatus?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Max(100) size: Int,
    ): ApiResponse<Page<AdminToiletResponse>> {
        val pageable = PageRequest.of(page, size)
        return ApiResponse.ok(adminService.getReports(status, pageable))
    }

    @Operation(summary = "제보 단건 조회")
    @GetMapping("/{id}")
    fun getReport(@PathVariable id: Long): ApiResponse<AdminToiletResponse> =
        ApiResponse.ok(adminService.getReport(id))

    @Operation(summary = "제보 승인 (PENDING → ACTIVE)")
    @PatchMapping("/{id}/approve")
    fun approveReport(@PathVariable id: Long): ApiResponse<AdminToiletResponse> =
        ApiResponse.ok(adminService.approveReport(id))

    @Operation(summary = "제보 거절 (PENDING → REJECTED)")
    @PatchMapping("/{id}/reject")
    fun rejectReport(@PathVariable id: Long): ApiResponse<AdminToiletResponse> =
        ApiResponse.ok(adminService.rejectReport(id))
}
