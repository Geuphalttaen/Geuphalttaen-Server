package com.geuphalttaen.core.entity

import jakarta.persistence.*

enum class SyncStatus {
    SUCCESS,
    PARTIAL,
    FAILED,
}

@Entity
@Table(name = "sync_logs")
class SyncLogEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "total_fetched", nullable = false)
    val totalFetched: Int = 0,

    @Column(name = "upserted_count", nullable = false)
    val upsertedCount: Int = 0,

    @Column(name = "failed_count", nullable = false)
    val failedCount: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: SyncStatus = SyncStatus.SUCCESS,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

) : BaseEntity()
