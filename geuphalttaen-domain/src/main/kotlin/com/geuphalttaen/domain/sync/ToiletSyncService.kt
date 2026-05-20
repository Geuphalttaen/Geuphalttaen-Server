package com.geuphalttaen.domain.sync

import com.geuphalttaen.core.entity.SyncLogEntity
import com.geuphalttaen.core.entity.SyncStatus
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.toilet.ToiletRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.charset.Charset

@Service
class ToiletSyncService(
    private val toiletDataPort: ToiletDataPort,
    private val toiletRepository: ToiletRepository,
    private val syncLogRepository: SyncLogRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CHUNK_SIZE = 500
    }

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

    fun syncFromUpload(inputStream: InputStream, charset: Charset = Charset.forName("EUC-KR")): SyncLogEntity {
        log.info("공공 화장실 데이터 동기화 시작 (파일 업로드)")

        val fetchResult = try {
            toiletDataPort.fetchFromStream(inputStream, charset)
        } catch (e: RuntimeException) {
            log.error("공공 화장실 데이터 파싱 실패: {}", e.message)
            return syncLogRepository.save(
                SyncLogEntity(
                    totalFetched = 0,
                    upsertedCount = 0,
                    insertedCount = 0,
                    updatedCount = 0,
                    deletedCount = 0,
                    failedCount = 0,
                    status = SyncStatus.FAILED,
                    errorMessage = e.message,
                ),
            )
        }

        return try {
            val externalData = fetchResult.items
            val totalFetched = externalData.size + fetchResult.parseFailCount
            log.info("공공 화장실 데이터 파싱 완료: 정상={}건, 파싱실패={}건", externalData.size, fetchResult.parseFailCount)

            var insertedCount = 0
            var updatedCount = 0
            var failedCount = fetchResult.parseFailCount

            // CHUNK_SIZE 단위로 쪼개어 처리 — IN 절 폭발 및 OOM 방지
            val processedAddresses = mutableSetOf<String>()
            for (chunk in externalData.chunked(CHUNK_SIZE)) {
                val addresses = chunk.map { it.address }
                val existingByAddress = toiletRepository.findAllByAddressIn(addresses)
                    .associateBy { it.address }

                val toSave = mutableListOf<ToiletEntity>()
                val chunkSuccessAddresses = mutableSetOf<String>()
                for (data in chunk) {
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
                        chunkSuccessAddresses.add(data.address)
                    } catch (e: Exception) {
                        log.error("화장실 upsert 준비 실패: address={}, error={}", data.address, e.message)
                        failedCount++
                    }
                }
                toiletRepository.saveAll(toSave)
                processedAddresses.addAll(chunkSuccessAddresses)
                log.info("청크 처리 완료: inserted={}, updated={} (누적)", insertedCount, updatedCount)
            }

            // CSV에서 사라진 공공 항목 삭제 — 전체 조회 후 인메모리 필터 (NOT IN 파라미터 폭발 방지)
            val deletedCount = if (processedAddresses.isNotEmpty()) {
                val stale = toiletRepository.findAllActivePublic()
                    .filter { it.address !in processedAddresses }
                if (stale.isNotEmpty()) {
                    toiletRepository.deleteAll(stale)
                    log.info("공공 화장실 삭제: {}건", stale.size)
                }
                stale.size
            } else {
                0
            }

            val status = when {
                insertedCount + updatedCount == 0 && failedCount > 0 -> SyncStatus.FAILED
                failedCount > 0 -> SyncStatus.PARTIAL
                else -> SyncStatus.SUCCESS
            }

            val syncLog = SyncLogEntity(
                totalFetched = totalFetched,
                upsertedCount = insertedCount + updatedCount,
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
            saved
        } catch (e: Exception) {
            log.error("공공 화장실 동기화 처리 중 오류 발생: {}", e.message)
            syncLogRepository.save(
                SyncLogEntity(
                    totalFetched = fetchResult.items.size + fetchResult.parseFailCount,
                    upsertedCount = 0,
                    insertedCount = 0,
                    updatedCount = 0,
                    deletedCount = 0,
                    failedCount = fetchResult.items.size + fetchResult.parseFailCount,
                    status = SyncStatus.FAILED,
                    errorMessage = e.message,
                ),
            )
        }
    }
}
