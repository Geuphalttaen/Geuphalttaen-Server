package com.geuphalttaen.infra.review

import com.geuphalttaen.core.entity.CleanlinessEntity
import com.geuphalttaen.domain.review.CleanlinessRepository
import org.springframework.stereotype.Repository

@Repository
class CleanlinessRepositoryImpl(
    private val jpaRepository: CleanlinessJpaRepository,
) : CleanlinessRepository {

    override fun findByToiletIdAndUserId(toiletId: Long, userId: Long): CleanlinessEntity? =
        jpaRepository.findByToiletIdAndUserId(toiletId, userId)

    override fun save(entity: CleanlinessEntity): CleanlinessEntity = jpaRepository.save(entity)

    override fun findAverageByToiletId(toiletId: Long): Double? =
        jpaRepository.findAverageByToiletId(toiletId)

    override fun findAveragesByToiletIds(toiletIds: List<Long>): Map<Long, Double> {
        if (toiletIds.isEmpty()) return emptyMap()
        return jpaRepository.findAveragesRawByToiletIds(toiletIds).associate { row ->
            // row[0]: toiletId(Long), row[1]: avgScore(Double)
            val toiletId = (row[0] as Number).toLong()
            val avg = (row[1] as Number).toDouble()
            toiletId to avg
        }
    }
}
