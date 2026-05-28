package com.geuphalttaen.infra.review

import com.geuphalttaen.core.entity.CleanlinessEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface CleanlinessJpaRepository : JpaRepository<CleanlinessEntity, Long> {
    fun findByToiletIdAndUserId(toiletId: Long, userId: Long): CleanlinessEntity?

    @Query("SELECT c.toiletId, AVG(c.score) FROM CleanlinessEntity c WHERE c.toiletId IN :toiletIds GROUP BY c.toiletId")
    fun findAveragesRawByToiletIds(@Param("toiletIds") toiletIds: List<Long>): List<Array<Any>>

    @Query("SELECT AVG(c.score) FROM CleanlinessEntity c WHERE c.toiletId = :toiletId")
    fun findAverageByToiletId(@Param("toiletId") toiletId: Long): Double?

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CleanlinessEntity c WHERE c.userId = :userId")
    fun deleteAllByUserId(@Param("userId") userId: Long)
}
