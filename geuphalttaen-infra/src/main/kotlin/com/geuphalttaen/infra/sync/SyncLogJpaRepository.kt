package com.geuphalttaen.infra.sync

import com.geuphalttaen.core.entity.SyncLogEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SyncLogJpaRepository : JpaRepository<SyncLogEntity, Long>
