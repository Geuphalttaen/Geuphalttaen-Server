package com.geuphalttaen.core.entity

import jakarta.persistence.*

enum class SyncStatus {
    IN_PROGRESS,
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
    var totalFetched: Int = 0,

    /** 기존 DB 컬럼 호환 유지 — insertedCount + updatedCount 와 동일한 값 */
    @Column(name = "upserted_count", nullable = false)
    var upsertedCount: Int = 0,

    @Column(name = "inserted_count", nullable = false)
    var insertedCount: Int = 0,

    @Column(name = "updated_count", nullable = false)
    var updatedCount: Int = 0,

    @Column(name = "deleted_count", nullable = false)
    var deletedCount: Int = 0,

    @Column(name = "failed_count", nullable = false)
    var failedCount: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SyncStatus = SyncStatus.IN_PROGRESS,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

) : BaseEntity()
