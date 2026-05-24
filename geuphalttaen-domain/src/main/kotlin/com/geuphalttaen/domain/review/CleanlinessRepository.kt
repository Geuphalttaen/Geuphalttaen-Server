package com.geuphalttaen.domain.review

import com.geuphalttaen.core.entity.CleanlinessEntity

interface CleanlinessRepository {
    fun findByToiletIdAndUserId(toiletId: Long, userId: Long): CleanlinessEntity?
    fun save(entity: CleanlinessEntity): CleanlinessEntity
    fun findAverageByToiletId(toiletId: Long): Double?
    fun findAveragesByToiletIds(toiletIds: List<Long>): Map<Long, Double>
}
