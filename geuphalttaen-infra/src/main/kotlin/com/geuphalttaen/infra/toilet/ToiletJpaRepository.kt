package com.geuphalttaen.infra.toilet

import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ToiletJpaRepository : JpaRepository<ToiletEntity, Long> {
    fun findByAddress(address: String): ToiletEntity?
    fun findAllByAddressIn(addresses: List<String>): List<ToiletEntity>
    fun findByReportedBy(reportedBy: Long): List<ToiletEntity>
    fun findByReportedByOrderByCreatedAtDesc(reportedBy: Long): List<ToiletEntity>
    fun countByReportedBy(reportedBy: Long): Long
    fun countByReportedByAndStatus(reportedBy: Long, status: ToiletStatus): Long
}
