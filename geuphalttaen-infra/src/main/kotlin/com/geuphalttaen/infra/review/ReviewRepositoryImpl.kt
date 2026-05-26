package com.geuphalttaen.infra.review

import com.geuphalttaen.core.entity.ReviewEntity
import com.geuphalttaen.domain.review.ReviewRepository
import com.geuphalttaen.domain.review.ReviewStats
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class ReviewRepositoryImpl(
    private val jpaRepository: ReviewJpaRepository,
) : ReviewRepository {

    override fun save(entity: ReviewEntity): ReviewEntity = jpaRepository.save(entity)

    override fun findById(id: Long): ReviewEntity? = jpaRepository.findById(id).orElse(null)

    override fun delete(entity: ReviewEntity) = jpaRepository.delete(entity)

    override fun existsByToiletIdAndUserId(toiletId: Long, userId: Long): Boolean =
        jpaRepository.existsByToiletIdAndUserId(toiletId, userId)

    override fun findByToiletIdPageable(toiletId: Long, pageable: Pageable): Page<ReviewEntity> =
        jpaRepository.findAllByToiletId(toiletId, pageable)

    override fun findStatsByToiletIds(toiletIds: List<Long>): Map<Long, ReviewStats> {
        if (toiletIds.isEmpty()) return emptyMap()
        return jpaRepository.findStatsRawByToiletIds(toiletIds).associate { row ->
            // row[0]: toiletId(Long), row[1]: avgRating(Double?), row[2]: count(Long)
            val toiletId = (row[0] as Number).toLong()
            val avg = (row[1] as? Number)?.toDouble()
            val count = (row[2] as Number).toLong()
            toiletId to ReviewStats(avg, count)
        }
    }

    override fun findStatsByToiletId(toiletId: Long): ReviewStats {
        val row = jpaRepository.findStatsByToiletId(toiletId)
        if (row.size < 2) return ReviewStats(null, 0L)
        val avg = (row[0] as? Number)?.toDouble()
        val count = (row[1] as? Number)?.toLong() ?: 0L
        return ReviewStats(avg, count)
    }
}
