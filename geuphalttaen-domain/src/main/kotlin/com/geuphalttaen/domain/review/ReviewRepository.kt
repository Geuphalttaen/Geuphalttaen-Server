package com.geuphalttaen.domain.review

import com.geuphalttaen.core.entity.ReviewEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ReviewRepository {
    fun save(entity: ReviewEntity): ReviewEntity
    fun findById(id: Long): ReviewEntity?
    fun delete(entity: ReviewEntity)
    fun existsByToiletIdAndUserId(toiletId: Long, userId: Long): Boolean
    fun findByToiletIdPageable(toiletId: Long, pageable: Pageable): Page<ReviewEntity>
    fun findStatsByToiletIds(toiletIds: List<Long>): Map<Long, ReviewStats>
    fun findStatsByToiletId(toiletId: Long): ReviewStats
}

data class ReviewStats(val averageRating: Double?, val reviewCount: Long)
