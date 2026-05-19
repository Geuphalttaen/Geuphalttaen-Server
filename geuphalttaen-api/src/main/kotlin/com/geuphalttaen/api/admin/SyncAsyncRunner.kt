package com.geuphalttaen.api.admin

import com.geuphalttaen.domain.sync.ToiletSyncService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * I6: 동기화 요청을 비동기로 실행하는 래퍼.
 * @Async 는 같은 Bean 의 자기 호출(self-invocation) 시 프록시를 우회하므로
 * 별도 컴포넌트로 분리한다.
 */
@Component
class SyncAsyncRunner(
    private val toiletSyncService: ToiletSyncService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun runSyncAll() {
        log.info("비동기 공공 화장실 동기화 시작")
        try {
            val result = toiletSyncService.syncAll()
            log.info("비동기 공공 화장실 동기화 완료: status={}, upserted={}", result.status, result.upsertedCount)
        } catch (e: Exception) {
            log.error("비동기 공공 화장실 동기화 실패: {}", e.message, e)
        }
    }
}
