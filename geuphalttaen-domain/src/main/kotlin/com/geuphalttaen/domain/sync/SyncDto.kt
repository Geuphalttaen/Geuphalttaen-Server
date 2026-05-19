package com.geuphalttaen.domain.sync

import java.time.LocalDateTime

data class SyncResultResponse(
    val id: Long,
    val status: String,
    val totalFetched: Int,
    val upsertedCount: Int,
    val failedCount: Int,
    val syncedAt: LocalDateTime,
    val errorMessage: String? = null,
)
