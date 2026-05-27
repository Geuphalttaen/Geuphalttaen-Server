package com.geuphalttaen.infra.toilet

import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

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
    @Modifying
    @Query("DELETE FROM ToiletEntity t WHERE t.address IN :addresses")
    fun deleteByAddressIn(addresses: Collection<String>)
}
