package com.geuphalttaen.domain.navigation

interface DirectionsPort {
    fun getDirections(startLat: Double, startLng: Double, endLat: Double, endLng: Double): DirectionsResult
}
