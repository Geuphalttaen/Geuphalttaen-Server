package com.geuphalttaen.domain.sync

import com.geuphalttaen.core.entity.SyncLogEntity

interface SyncLogRepository {
    fun save(entity: SyncLogEntity): SyncLogEntity
    fun findTopNByOrderByCreatedAtDesc(n: Int): List<SyncLogEntity>
}
