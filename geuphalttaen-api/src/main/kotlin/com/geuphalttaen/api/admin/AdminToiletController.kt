package com.geuphalttaen.api.admin

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.admin.AdminService
import com.geuphalttaen.domain.toilet.AdminToiletResponse
import com.geuphalttaen.domain.toilet.AdminToiletUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 관리자 화장실 CRUD 컨트롤러.
 */
@Tag(name = "Admin Toilets", description = "관리자 화장실 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/toilets")
class AdminToiletController(
    private val adminService: AdminService,
) {

    @Operation(summary = "화장실 목록 조회 (키워드 검색, 페이징)")
    @GetMapping
    fun getToilets(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<AdminToiletResponse>> {
        val pageable = PageRequest.of(page, size)
        return ApiResponse.ok(adminService.getToilets(keyword, pageable))
    }

    @Operation(summary = "화장실 단건 조회")
    @GetMapping("/{id}")
    fun getToilet(@PathVariable id: Long): ApiResponse<AdminToiletResponse> =
        ApiResponse.ok(adminService.getToilet(id))

    @Operation(summary = "화장실 정보 수정 (부분 수정 가능)")
    @PatchMapping("/{id}")
    fun updateToilet(
        @PathVariable id: Long,
        @RequestBody request: AdminToiletUpdateRequest,
    ): ApiResponse<AdminToiletResponse> =
        ApiResponse.ok(adminService.updateToilet(id, request))

    @Operation(summary = "화장실 삭제")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteToilet(@PathVariable id: Long): ApiResponse<Unit> {
        adminService.deleteToilet(id)
        return ApiResponse.ok()
    }
}
