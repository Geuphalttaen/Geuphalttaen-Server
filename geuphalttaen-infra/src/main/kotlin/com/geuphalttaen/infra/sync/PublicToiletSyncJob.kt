package com.geuphalttaen.infra.sync

import com.geuphalttaen.domain.sync.ToiletSyncService
import com.geuphalttaen.infra.opendata.PublicToiletApiClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PublicToiletSyncJob(
    private val publicToiletApiClient: PublicToiletApiClient,
    private val toiletSyncService: ToiletSyncService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * MON")
    fun syncWeekly() {
        log.info("공공데이터 화장실 주간 동기화 시작")
        try {
            val toilets = publicToiletApiClient.fetchAllToilets()
            val result = toiletSyncService.syncPublicToilets(toilets)
            log.info("공공데이터 화장실 주간 동기화 완료: created={}, updated={}, skipped={}, syncedAt={}", result.created, result.updated, result.skipped, result.syncedAt)
        } catch (e: Exception) {
            log.error("공공데이터 화장실 주간 동기화 실패", e)
        }
    }
}
