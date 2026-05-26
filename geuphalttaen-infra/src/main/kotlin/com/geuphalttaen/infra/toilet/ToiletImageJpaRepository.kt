package com.geuphalttaen.infra.toilet

import com.geuphalttaen.core.entity.ToiletImageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ToiletImageJpaRepository : JpaRepository<ToiletImageEntity, Long> {
    fun findAllByToiletId(toiletId: Long): List<ToiletImageEntity>
    fun findAllByToiletIdIn(toiletIds: List<Long>): List<ToiletImageEntity>
}
