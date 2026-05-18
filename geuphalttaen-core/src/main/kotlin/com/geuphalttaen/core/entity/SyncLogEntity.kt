package com.geuphalttaen.core.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "sync_logs")
class SyncLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "synced_at", nullable = false)
    val syncedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_count", nullable = false)
    val createdCount: Int = 0,

    @Column(name = "updated_count", nullable = false)
    val updatedCount: Int = 0,

    @Column(name = "skipped_count", nullable = false)
    val skippedCount: Int = 0,
)
