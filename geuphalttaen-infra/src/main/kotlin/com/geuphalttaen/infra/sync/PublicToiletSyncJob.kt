package com.geuphalttaen.infra.sync

import com.geuphalttaen.domain.sync.ToiletSyncService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PublicToiletSyncJob(
    private val toiletSyncService: ToiletSyncService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 0 * * *")
    fun syncWeekly() {
        log.info("공공데이터 화장실 주간 동기화 시작")
        try {
            val result = toiletSyncService.syncAll()
            log.info(
                "공공데이터 화장실 주간 동기화 완료: total={}, inserted={}, updated={}, deleted={}, failed={}, status={}",
                result.totalFetched, result.insertedCount, result.updatedCount, result.deletedCount, result.failedCount, result.status,
            )
        } catch (e: Exception) {
            log.error("공공데이터 화장실 주간 동기화 실패", e)
        }
    }
}
