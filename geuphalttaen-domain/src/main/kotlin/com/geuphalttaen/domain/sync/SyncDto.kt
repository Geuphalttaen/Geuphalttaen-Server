package com.geuphalttaen.domain.sync

import java.time.LocalDateTime

data class SyncResultResponse(
    val id: Long,
    val status: String,
    val totalFetched: Int,
    val insertedCount: Int,
    val updatedCount: Int,
    val deletedCount: Int,
    val failedCount: Int,
    val syncedAt: LocalDateTime,
    val errorMessage: String? = null,
)
