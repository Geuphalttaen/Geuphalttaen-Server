package com.geuphalttaen.domain.sync

import java.time.LocalDateTime

data class SyncToiletDto(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val male: Boolean,
    val female: Boolean,
    val disabled: Boolean,
    val familyRoom: Boolean = false,
)

data class SyncResult(
    val created: Int,
    val updated: Int,
    val skipped: Int,
    val syncedAt: LocalDateTime,
)

data class SyncStatusResponse(
    val lastSyncedAt: LocalDateTime,
    val createdCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
)
