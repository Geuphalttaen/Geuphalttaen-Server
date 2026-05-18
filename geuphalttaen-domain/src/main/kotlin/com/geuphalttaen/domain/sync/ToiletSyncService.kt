package com.geuphalttaen.domain.sync

import com.geuphalttaen.core.entity.SyncLogEntity
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.toilet.ToiletRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ToiletSyncService(
    private val toiletRepository: ToiletRepository,
    private val syncLogRepository: SyncLogRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun syncPublicToilets(toilets: List<SyncToiletDto>): SyncResult {
        var created = 0
        var updated = 0
        var skipped = 0

        for (dto in toilets) {
            if (dto.lat == 0.0 || dto.lng == 0.0) {
                skipped++
                continue
            }

            val existing = toiletRepository.findByNameAndAddress(dto.name, dto.address)
            if (existing != null) {
                existing.lat = dto.lat
                existing.lng = dto.lng
                existing.male = dto.male
                existing.female = dto.female
                existing.disabled = dto.disabled
                existing.familyRoom = dto.familyRoom
                toiletRepository.save(existing)
                updated++
            } else {
                val entity = ToiletEntity(
                    name = dto.name,
                    address = dto.address,
                    lat = dto.lat,
                    lng = dto.lng,
                    isPublic = true,
                    male = dto.male,
                    female = dto.female,
                    disabled = dto.disabled,
                    familyRoom = dto.familyRoom,
                    status = ToiletStatus.ACTIVE,
                )
                toiletRepository.save(entity)
                created++
            }
        }

        val syncedAt = LocalDateTime.now()
        syncLogRepository.save(
            SyncLogEntity(
                syncedAt = syncedAt,
                createdCount = created,
                updatedCount = updated,
                skippedCount = skipped,
            ),
        )

        log.info("화장실 동기화 완료: created={}, updated={}, skipped={}", created, updated, skipped)
        return SyncResult(created = created, updated = updated, skipped = skipped, syncedAt = syncedAt)
    }

    fun getLastSyncStatus(): SyncStatusResponse? {
        val entry = syncLogRepository.findTopByOrderBySyncedAtDesc() ?: return null
        return SyncStatusResponse(
            lastSyncedAt = entry.syncedAt,
            createdCount = entry.createdCount,
            updatedCount = entry.updatedCount,
            skippedCount = entry.skippedCount,
        )
    }
}
