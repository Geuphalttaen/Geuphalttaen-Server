package com.geuphalttaen.infra.sync

import com.geuphalttaen.core.entity.SyncLogEntity
import com.geuphalttaen.domain.sync.SyncLogRepository
import org.springframework.stereotype.Repository

@Repository
class SyncLogRepositoryImpl(
    private val jpaRepository: SyncLogJpaRepository,
) : SyncLogRepository {
    override fun save(entity: SyncLogEntity): SyncLogEntity = jpaRepository.save(entity)
    override fun findTopByOrderBySyncedAtDesc(): SyncLogEntity? = jpaRepository.findTopByOrderBySyncedAtDesc()
}
