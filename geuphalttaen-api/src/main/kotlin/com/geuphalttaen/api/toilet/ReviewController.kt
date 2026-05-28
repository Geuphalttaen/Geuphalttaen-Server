package com.geuphalttaen.api.toilet

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.review.CleanlinessRequest
import com.geuphalttaen.domain.review.CleanlinessResponse
import com.geuphalttaen.domain.review.CleanlinessService
import com.geuphalttaen.domain.review.ReviewRequest
import com.geuphalttaen.domain.review.ReviewResponse
import com.geuphalttaen.domain.review.ReviewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "Review", description = "리뷰 및 청결도 평가 API")
@RestController
@RequestMapping("/api/v1/toilets/{id}")
@Validated
class ReviewController(
    private val reviewService: ReviewService,
    private val cleanlinessService: CleanlinessService,
) {
    @Operation(summary = "리뷰 작성 (인증 필요)", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping("/reviews")
    fun addReview(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: ReviewRequest,
    ): ApiResponse<ReviewResponse> {
        return ApiResponse.ok(reviewService.addReview(userId, id, request))
    }

    @Operation(summary = "내 리뷰 조회 (인증 필요) — 작성한 리뷰가 없으면 null 반환", security = [SecurityRequirement(name = "bearerAuth")])
    @GetMapping("/reviews/my")
    fun getMyReview(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
    ): ApiResponse<ReviewResponse?> {
        return ApiResponse.ok(reviewService.getMyReview(userId, id))
    }

    @Operation(summary = "내 리뷰 수정 (인증 필요)", security = [SecurityRequirement(name = "bearerAuth")])
    @PatchMapping("/reviews/my")
    fun updateMyReview(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: ReviewRequest,
    ): ApiResponse<ReviewResponse> {
        return ApiResponse.ok(reviewService.updateMyReview(userId, id, request))
    }

    @Operation(summary = "리뷰 목록 조회 (인증 불필요)")
    @GetMapping("/reviews")
    fun getReviews(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "10") @Max(100) size: Int,
    ): ApiResponse<Page<ReviewResponse>> {
        val pageable = PageRequest.of(page, size)
        return ApiResponse.ok(reviewService.getReviews(id, pageable))
    }

    @Operation(summary = "청결도 평가 (인증 필요) — 이미 평가한 경우 덮어쓴다", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping("/cleanliness")
    fun upsertCleanliness(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: CleanlinessRequest,
    ): ApiResponse<CleanlinessResponse> {
        return ApiResponse.ok(cleanlinessService.upsert(userId, id, request))
    }
}
