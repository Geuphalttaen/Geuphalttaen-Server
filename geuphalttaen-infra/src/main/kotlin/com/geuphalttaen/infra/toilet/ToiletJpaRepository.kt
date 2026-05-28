package com.geuphalttaen.infra.toilet

import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface ToiletJpaRepository : JpaRepository<ToiletEntity, Long> {
    fun findByAddress(address: String): ToiletEntity?
    fun findAllByAddressIn(addresses: List<String>): List<ToiletEntity>
    fun findByReportedByOrderByCreatedAtDesc(reportedBy: Long): List<ToiletEntity>
    fun countByReportedBy(reportedBy: Long): Long
    fun countByReportedByAndStatus(reportedBy: Long, status: ToiletStatus): Long
    fun countByStatus(status: ToiletStatus): Long

    @Query("SELECT t.address FROM ToiletEntity t WHERE t.reportedBy IS NULL AND t.status = :status")
    fun findAllAddressByReportedByIsNullAndStatus(status: ToiletStatus): List<String>

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ToiletEntity t WHERE t.address IN :addresses")
    fun deleteByAddressIn(addresses: Collection<String>)

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ToiletEntity t SET t.reportedBy = null WHERE t.reportedBy = :userId")
    fun nullifyReportedBy(@Param("userId") userId: Long)
}
