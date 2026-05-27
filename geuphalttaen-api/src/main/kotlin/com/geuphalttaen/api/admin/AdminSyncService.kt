package com.geuphalttaen.api.admin

import com.geuphalttaen.core.entity.SyncLogEntity
import com.geuphalttaen.domain.sync.SyncAsyncWorker
import com.geuphalttaen.domain.sync.ToiletSyncService
import org.springframework.stereotype.Service
import java.nio.charset.Charset

@Service
class AdminSyncService(
    private val toiletSyncService: ToiletSyncService,
    private val syncAsyncWorker: SyncAsyncWorker,
) {
    fun startUploadAsync(fileBytes: ByteArray, charset: Charset): SyncLogEntity {
        val syncLog = toiletSyncService.createInProgressLog()
        syncAsyncWorker.run(syncLog.id, fileBytes, charset)
        return syncLog
    }
}
