package com.geuphalttaen.domain.geocoding

interface GeocodingPort {
    fun reverseGeocode(lat: Double, lng: Double): String
}
