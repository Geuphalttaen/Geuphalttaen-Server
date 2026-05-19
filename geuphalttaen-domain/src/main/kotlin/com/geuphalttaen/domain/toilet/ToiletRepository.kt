package com.geuphalttaen.domain.toilet

import com.geuphalttaen.core.entity.ToiletEntity

interface ToiletRepository {
    fun findNearby(lat: Double, lng: Double, radiusMeters: Int): List<ToiletEntity>
    fun findById(id: Long): ToiletEntity?
    fun findByAddress(address: String): ToiletEntity?
    fun findAllByAddressIn(addresses: List<String>): List<ToiletEntity>
    fun findByReportedBy(reportedBy: Long): List<ToiletEntity>
    fun save(entity: ToiletEntity): ToiletEntity
    fun saveAll(entities: List<ToiletEntity>): List<ToiletEntity>
}
