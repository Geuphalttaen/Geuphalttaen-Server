package com.geuphalttaen.domain.sync

data class SyncResultResponse(
    val id: Long,
    val status: String,
    val totalFetched: Int,
    val upsertedCount: Int,
    val failedCount: Int,
)
