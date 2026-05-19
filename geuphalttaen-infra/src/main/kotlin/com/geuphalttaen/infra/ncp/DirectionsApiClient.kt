package com.geuphalttaen.infra.ncp

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

data class DirectionsResult(
    val path: List<LatLng>,
    val distanceMeters: Int,
    val durationMs: Int,
)

data class LatLng(val lat: Double, val lng: Double)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class NcpDirectionsResponse(val route: Map<String, List<RouteOption>>?)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class RouteOption(val summary: RouteSummary?, val path: List<List<Double>>?)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class RouteSummary(val distance: Int?, val duration: Int?)

@Component
class DirectionsApiClient(private val ncpProperties: NcpProperties) {

    private val restClient = RestClient.builder()
        .baseUrl("https://naveropenapi.apigw.ntruss.com")
        .defaultHeader("X-NCP-APIGW-API-KEY-ID", ncpProperties.clientId)
        .defaultHeader("X-NCP-APIGW-API-KEY", ncpProperties.clientSecret)
        .build()

    fun getDirections(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double,
    ): DirectionsResult {
        val response = restClient.get()
            .uri("/map-direction/v1/driving?start={s}&goal={g}&option=traoptimal",
                "$startLng,$startLat", "$endLng,$endLat")
            .retrieve()
            .body<NcpDirectionsResponse>()

        val route = response?.route
            ?.values
            ?.firstOrNull()
            ?.firstOrNull()
            ?: throw BusinessException(ErrorCode.ROUTE_NOT_FOUND)

        val path = route.path?.map { LatLng(lat = it[1], lng = it[0]) } ?: emptyList()
        return DirectionsResult(
            path = path,
            distanceMeters = route.summary?.distance ?: 0,
            durationMs = route.summary?.duration ?: 0,
        )
    }
}
