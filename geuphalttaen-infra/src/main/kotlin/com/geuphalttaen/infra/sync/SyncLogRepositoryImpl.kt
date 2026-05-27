package com.geuphalttaen.infra.sync

import com.geuphalttaen.core.entity.SyncLogEntity
import com.geuphalttaen.domain.sync.SyncLogRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.stereotype.Repository

@Repository
class SyncLogRepositoryImpl(
    private val jpaRepository: SyncLogJpaRepository,
) : SyncLogRepository {

    override fun save(entity: SyncLogEntity): SyncLogEntity = jpaRepository.save(entity)

    override fun findById(id: Long): SyncLogEntity? = jpaRepository.findById(id).orElse(null)

    // I4: PageRequest 로 DB 레벨 LIMIT 적용 — 전체 스캔 후 take(n) 방식 제거
    override fun findTopNByOrderByCreatedAtDesc(n: Int): List<SyncLogEntity> =
        jpaRepository.findAll(PageRequest.of(0, n, Sort.by(DESC, "createdAt"))).content
}
