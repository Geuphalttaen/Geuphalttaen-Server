package com.geuphalttaen.domain.toilet

import com.geuphalttaen.core.entity.ToiletEntity

interface ToiletRepository {
    fun findNearby(lat: Double, lng: Double, radiusMeters: Int): List<ToiletEntity>
    fun save(entity: ToiletEntity): ToiletEntity
}
