package com.geuphalttaen.api.toilet

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.image.ImageService
import com.geuphalttaen.domain.toilet.ImageUploadResponse
import com.geuphalttaen.domain.toilet.ToiletReportRequest
import com.geuphalttaen.domain.toilet.ToiletResponse
import com.geuphalttaen.domain.toilet.ToiletSearchRequest
import com.geuphalttaen.domain.toilet.ToiletService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Toilet", description = "공중화장실 API")
@RestController
@RequestMapping("/api/v1/toilets")
class ToiletController(
    private val toiletService: ToiletService,
    private val imageService: ImageService,
) {
    @Operation(summary = "근처 화장실 검색 (인증 불필요)")
    @GetMapping
    fun searchNearby(
        @Valid @ModelAttribute request: ToiletSearchRequest,
    ): ApiResponse<List<ToiletResponse>> {
        val results = toiletService.searchNearby(request)
        return ApiResponse.ok(results)
    }

    @Operation(summary = "화장실 상세 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "화장실을 찾을 수 없음")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ApiResponse<ToiletResponse> {
        return ApiResponse.ok(toiletService.getById(id))
    }

    @Operation(summary = "이미지 업로드 — 원본 저장 + WebP 변환 후 양쪽 URL 반환 (인증 필요)")
    @PostMapping("/images/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @AuthenticationPrincipal userId: Long,
        @RequestParam("file") file: MultipartFile,
    ): ApiResponse<ImageUploadResponse> {
        val contentType = file.contentType ?: "application/octet-stream"
        val result = imageService.upload(file.bytes, contentType, userId)
        return ApiResponse.ok(ImageUploadResponse(url = result.url, originalUrl = result.originalUrl))
    }

    @Operation(summary = "화장실 제보 (인증 필요)")
    @PostMapping("/report")
    fun report(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: ToiletReportRequest,
    ): ApiResponse<ToiletResponse> {
        val result = toiletService.report(userId, request)
        return ApiResponse.ok(result)
    }
}
