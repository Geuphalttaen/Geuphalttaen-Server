package com.geuphalttaen.api.directions

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.navigation.DirectionsPort
import com.geuphalttaen.domain.navigation.DirectionsResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Directions", description = "경로 탐색 API")
@RestController
@RequestMapping("/api/v1/directions")
class DirectionsController(private val directionsPort: DirectionsPort) {

    @Operation(summary = "출발지 → 목적지 경로 조회 (인증 불필요)")
    @GetMapping
    fun getDirections(
        @RequestParam startLat: Double,
        @RequestParam startLng: Double,
        @RequestParam endLat: Double,
        @RequestParam endLng: Double,
    ): ApiResponse<DirectionsResult> =
        ApiResponse.ok(directionsPort.getDirections(startLat, startLng, endLat, endLng))
}
