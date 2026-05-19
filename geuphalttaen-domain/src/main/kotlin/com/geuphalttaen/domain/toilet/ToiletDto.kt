package com.geuphalttaen.domain.toilet

data class ToiletSearchRequest(
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int = 1000,
)

data class ToiletResponse(
    val id: Long,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val distanceMeters: Double?,
    val male: Boolean,
    val female: Boolean,
    val disabled: Boolean,
    val familyRoom: Boolean,
    val isPublic: Boolean,
)

data class ToiletReportRequest(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val isPublic: Boolean = true,
    val male: Boolean = true,
    val female: Boolean = true,
    val disabled: Boolean = false,
    val familyRoom: Boolean = false,
)
