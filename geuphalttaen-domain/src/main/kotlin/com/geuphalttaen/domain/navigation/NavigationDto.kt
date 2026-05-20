package com.geuphalttaen.domain.navigation

data class DirectionsResult(
    val path: List<LatLng>,
    val distanceMeters: Int,
    val durationMs: Int,
)

data class LatLng(val lat: Double, val lng: Double)
