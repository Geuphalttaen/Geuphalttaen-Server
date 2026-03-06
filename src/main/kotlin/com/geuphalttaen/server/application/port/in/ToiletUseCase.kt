package com.geuphalttaen.server.application.port.`in`

import com.geuphalttaen.server.domain.Toilet

interface ToiletUseCase {
    fun findNearbyToilets(latitude: Double, longitude: Double, radiusKm: Double): List<Toilet>
    fun findById(id: Long): Toilet
}
