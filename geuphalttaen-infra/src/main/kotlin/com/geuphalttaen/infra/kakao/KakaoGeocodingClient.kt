package com.geuphalttaen.infra.kakao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.geuphalttaen.domain.geocoding.GeocodingPort
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KakaoReverseGeocodeResponse(val documents: List<KakaoDocument>?)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KakaoDocument(
    val road_address: KakaoAddress?,
    val address: KakaoAddress?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KakaoAddress(val address_name: String?, val building_name: String?)

@Component
class KakaoGeocodingClient(private val kakaoMapsProperties: KakaoMapsProperties) : GeocodingPort {

    private val restClient = RestClient.builder()
        .baseUrl("https://dapi.kakao.com")
        .defaultHeader("Authorization", "KakaoAK ${kakaoMapsProperties.restKey}")
        .build()

    override fun reverseGeocode(lat: Double, lng: Double): String {
        val response = restClient.get()
            .uri("/v2/local/geo/coord2address.json?x={x}&y={y}&input_coord=WGS84", lng, lat)
            .retrieve()
            .body<KakaoReverseGeocodeResponse>()

        val doc = response?.documents?.firstOrNull() ?: return ""
        val addr = doc.road_address ?: doc.address ?: return ""
        val base = addr.address_name ?: return ""
        val building = addr.building_name?.takeIf { it.isNotBlank() }
        return if (building != null) "$base ($building)" else base
    }
}
