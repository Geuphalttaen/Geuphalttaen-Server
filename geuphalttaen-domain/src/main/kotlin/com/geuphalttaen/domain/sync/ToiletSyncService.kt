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

    /**
     * 공공데이터 전체 화장실을 가져와 upsert 처리하고 SyncLogEntity를 저장 후 반환한다.
     * API 호출 실패 시 FAILED 상태의 SyncLogEntity를 저장하고 반환한다.
     */
    fun getSyncLogs(n: Int = 10): List<SyncResultResponse> =
        syncLogRepository.findTopNByOrderByCreatedAtDesc(n).map {
            SyncResultResponse(
                id = it.id,
                status = it.status.name,
                totalFetched = it.totalFetched,
                upsertedCount = it.upsertedCount,
                failedCount = it.failedCount,
                syncedAt = it.createdAt,
                errorMessage = it.errorMessage,
            )
        }

    /**
     * @Transactional 범위 내에서 API 장애 시 early-return 으로 FAILED 로그를 저장한다.
     * RuntimeException catch 후 syncLogRepository.save() 는 같은 트랜잭션 내에서 실행되어 원자성이 보장된다.
     */
    @Transactional
    fun syncAll(): SyncLogEntity {
        log.info("공공 화장실 데이터 동기화 시작")

        // API 호출 실패 시 FAILED 로그 저장 후 early-return
        val fetchResult = try {
            toiletDataPort.fetchAllToilets()
        } catch (e: RuntimeException) {
            log.error("공공 화장실 데이터 조회 실패: {}", e.message)
            return syncLogRepository.save(
                SyncLogEntity(
                    totalFetched = 0,
                    upsertedCount = 0,
                    failedCount = 0,
                    status = SyncStatus.FAILED,
                    errorMessage = e.message,
                ),
            )
        }

        val externalData = fetchResult.items
        // I7: 파싱 실패 건수를 failedCount 에 포함
        val totalFetched = externalData.size + fetchResult.parseFailCount
        log.info("공공 화장실 데이터 조회 완료: 정상={}건, 파싱실패={}건", externalData.size, fetchResult.parseFailCount)

        // I5: 배치 조회로 N+1 제거
        val addresses = externalData.map { it.address }
        val existingByAddress = toiletRepository.findAllByAddressIn(addresses)
            .associateBy { it.address }

        val toSave = mutableListOf<ToiletEntity>()
        var upsertedCount = 0
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
                }
                upsertedCount++
            } catch (e: Exception) {
                log.error("화장실 upsert 준비 실패: address={}, error={}", data.address, e.message)
                failedCount++
            }
        }

        toiletRepository.saveAll(toSave)

        val status = when {
            failedCount == 0 -> SyncStatus.SUCCESS
            upsertedCount == 0 -> SyncStatus.FAILED
            else -> SyncStatus.PARTIAL
        }

        val syncLog = SyncLogEntity(
            totalFetched = totalFetched,
            upsertedCount = upsertedCount,
            failedCount = failedCount,
            status = status,
        )
        val saved = syncLogRepository.save(syncLog)

        log.info(
            "공공 화장실 데이터 동기화 완료: total={}, upserted={}, failed={}, status={}",
            totalFetched, upsertedCount, failedCount, status,
        )
        return saved
    }
}
