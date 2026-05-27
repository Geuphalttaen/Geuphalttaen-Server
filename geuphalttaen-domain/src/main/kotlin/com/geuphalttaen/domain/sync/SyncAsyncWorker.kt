package com.geuphalttaen.domain.sync

import com.geuphalttaen.core.entity.SyncStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.nio.charset.Charset

@Component
class SyncAsyncWorker(
    private val toiletSyncService: ToiletSyncService,
    private val syncLogRepository: SyncLogRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun run(syncLogId: Long, fileBytes: ByteArray, charset: Charset) {
        log.info("비동기 동기화 시작: syncLogId={}", syncLogId)
        val syncLog = syncLogRepository.findById(syncLogId) ?: run {
            log.error("동기화 로그를 찾을 수 없음: id={}", syncLogId)
            return
        }
        try {
            val result = toiletSyncService.runSync(fileBytes, charset)
            syncLog.totalFetched = result.totalFetched
            syncLog.upsertedCount = result.upsertedCount
            syncLog.insertedCount = result.insertedCount
            syncLog.updatedCount = result.updatedCount
            syncLog.deletedCount = result.deletedCount
            syncLog.failedCount = result.failedCount
            syncLog.status = result.status
            syncLog.errorMessage = result.errorMessage
            syncLogRepository.save(syncLog)
            log.info("비동기 동기화 완료: syncLogId={}, status={}", syncLogId, result.status)
        } catch (e: Exception) {
            log.error("비동기 동기화 중 예기치 못한 오류: syncLogId={}, error={}", syncLogId, e.message)
            syncLog.status = SyncStatus.FAILED
            syncLog.errorMessage = e.message
            syncLogRepository.save(syncLog)
        }
    }
}
