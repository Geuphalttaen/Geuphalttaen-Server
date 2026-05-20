package com.geuphalttaen.domain.sync

import com.geuphalttaen.core.entity.SyncLogEntity
import com.geuphalttaen.core.entity.SyncStatus
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.toilet.ToiletRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ToiletSyncService(
    private val toiletDataPort: ToiletDataPort,
    private val toiletRepository: ToiletRepository,
    private val syncLogRepository: SyncLogRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getSyncLogs(n: Int = 10): List<SyncResultResponse> =
        syncLogRepository.findTopNByOrderByCreatedAtDesc(n).map {
            SyncResultResponse(
                id = it.id,
                status = it.status.name,
                totalFetched = it.totalFetched,
                insertedCount = it.insertedCount,
                updatedCount = it.updatedCount,
                deletedCount = it.deletedCount,
                failedCount = it.failedCount,
                syncedAt = it.createdAt,
                errorMessage = it.errorMessage,
            )
        }

    @Transactional
    fun syncAll(): SyncLogEntity {
        log.info("공공 화장실 데이터 동기화 시작")

        val fetchResult = try {
            toiletDataPort.fetchAllToilets()
        } catch (e: RuntimeException) {
            log.error("공공 화장실 데이터 조회 실패: {}", e.message)
            return syncLogRepository.save(
                SyncLogEntity(
                    totalFetched = 0,
                    insertedCount = 0,
                    updatedCount = 0,
                    deletedCount = 0,
                    failedCount = 0,
                    status = SyncStatus.FAILED,
                    errorMessage = e.message,
                ),
            )
        }

        val externalData = fetchResult.items
        val totalFetched = externalData.size + fetchResult.parseFailCount
        log.info("공공 화장실 데이터 조회 완료: 정상={}건, 파싱실패={}건", externalData.size, fetchResult.parseFailCount)

        val addresses = externalData.map { it.address }
        val existingByAddress = toiletRepository.findAllByAddressIn(addresses)
            .associateBy { it.address }

        val toSave = mutableListOf<ToiletEntity>()
        var insertedCount = 0
        var updatedCount = 0
        var failedCount = fetchResult.parseFailCount

        for (data in externalData) {
            try {
                val existing = existingByAddress[data.address]
                if (existing != null) {
                    existing.name = data.name
                    existing.lat = data.lat
                    existing.lng = data.lng
                    existing.male = data.male
                    existing.female = data.female
                    existing.disabled = data.disabled
                    existing.familyRoom = data.familyRoom
                    existing.isPublic = true
                    existing.status = ToiletStatus.ACTIVE
                    toSave.add(existing)
                    updatedCount++
                } else {
                    toSave.add(
                        ToiletEntity(
                            name = data.name,
                            address = data.address,
                            lat = data.lat,
                            lng = data.lng,
                            isPublic = true,
                            male = data.male,
                            female = data.female,
                            disabled = data.disabled,
                            familyRoom = data.familyRoom,
                            status = ToiletStatus.ACTIVE,
                        ),
                    )
                    insertedCount++
                }
            } catch (e: Exception) {
                log.error("화장실 upsert 준비 실패: address={}, error={}", data.address, e.message)
                failedCount++
            }
        }

        toiletRepository.saveAll(toSave)

        // CSV에서 사라진 공공데이터 항목 삭제 (reportedBy=null이고 ACTIVE인 것 중 이번 CSV에 없는 것)
        val deletedCount = if (addresses.isNotEmpty()) {
            val stale = toiletRepository.findAllActivePublicNotInAddresses(addresses)
            toiletRepository.deleteAll(stale)
            log.info("공공 화장실 삭제: {}건", stale.size)
            stale.size
        } else {
            0
        }

        val status = when {
            failedCount == 0 && deletedCount == 0 -> SyncStatus.SUCCESS
            failedCount == 0 -> SyncStatus.SUCCESS
            insertedCount + updatedCount == 0 -> SyncStatus.FAILED
            else -> SyncStatus.PARTIAL
        }

        val syncLog = SyncLogEntity(
            totalFetched = totalFetched,
            insertedCount = insertedCount,
            updatedCount = updatedCount,
            deletedCount = deletedCount,
            failedCount = failedCount,
            status = status,
        )
        val saved = syncLogRepository.save(syncLog)

        log.info(
            "공공 화장실 동기화 완료: total={}, inserted={}, updated={}, deleted={}, failed={}, status={}",
            totalFetched, insertedCount, updatedCount, deletedCount, failedCount, status,
        )
        return saved
    }
}
