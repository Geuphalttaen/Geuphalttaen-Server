package com.geuphalttaen.infra.review

import com.geuphalttaen.core.entity.ReviewEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReviewJpaRepository : JpaRepository<ReviewEntity, Long> {
    fun existsByToiletIdAndUserId(toiletId: Long, userId: Long): Boolean
    fun findByToiletIdAndUserId(toiletId: Long, userId: Long): ReviewEntity?
    fun findAllByToiletId(toiletId: Long, pageable: Pageable): Page<ReviewEntity>

    @Query("SELECT r.toiletId, AVG(r.rating), COUNT(r) FROM ReviewEntity r WHERE r.toiletId IN :toiletIds GROUP BY r.toiletId")
    fun findStatsRawByToiletIds(@Param("toiletIds") toiletIds: List<Long>): List<Array<Any>>

    @Query("SELECT AVG(r.rating), COALESCE(COUNT(r), 0) FROM ReviewEntity r WHERE r.toiletId = :toiletId")
    fun findStatsByToiletId(@Param("toiletId") toiletId: Long): Array<Any?>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ReviewEntity r WHERE r.userId = :userId")
    fun deleteAllByUserId(@Param("userId") userId: Long)
}
