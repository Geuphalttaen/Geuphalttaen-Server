package com.geuphalttaen.api.admin

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.review.ReviewResponse
import com.geuphalttaen.domain.review.ReviewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "Admin Reviews", description = "관리자 리뷰 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@Validated
class AdminReviewController(
    private val reviewService: ReviewService,
) {
    @Operation(summary = "화장실 리뷰 목록 조회 (관리자)")
    @GetMapping("/api/v1/admin/toilets/{id}/reviews")
    fun getReviews(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "10") @Max(100) size: Int,
    ): ApiResponse<Page<ReviewResponse>> {
        val pageable = PageRequest.of(page, size)
        return ApiResponse.ok(reviewService.getReviewsForAdmin(id, pageable))
    }

    @Operation(summary = "리뷰 삭제 (관리자)")
    @DeleteMapping("/api/v1/admin/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteReview(@PathVariable id: Long) {
        reviewService.deleteReview(id)
    }
}
