package com.geuphalttaen.server.application.port.out

import com.geuphalttaen.server.domain.Toilet

interface ToiletRepository {
    fun findNearby(latitude: Double, longitude: Double, radiusKm: Double): List<Toilet>
    fun findById(id: Long): Toilet?
    fun saveAll(toilets: List<Toilet>): List<Toilet>
    fun count(): Long
}
