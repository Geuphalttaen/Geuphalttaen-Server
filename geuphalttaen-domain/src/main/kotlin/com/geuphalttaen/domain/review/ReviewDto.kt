package com.geuphalttaen.domain.review

import com.geuphalttaen.core.entity.ReviewEntity
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class ReviewRequest(
    @field:Min(1) @field:Max(5) val rating: Int,
    @field:Size(max = 200) val content: String? = null,
)

data class ReviewResponse(
    val id: Long,
    val toiletId: Long,
    val userId: Long,
    val rating: Int,
    val content: String?,
    val createdAt: LocalDateTime,
)

data class CleanlinessRequest(
    @field:Min(1) @field:Max(5) val score: Int,
)

data class CleanlinessResponse(
    val toiletId: Long,
    val userId: Long,
    val score: Int,
)

fun ReviewEntity.toResponse() = ReviewResponse(id, toiletId, userId, rating, content, createdAt)
