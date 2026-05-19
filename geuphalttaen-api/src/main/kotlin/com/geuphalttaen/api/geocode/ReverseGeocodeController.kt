package com.geuphalttaen.api.geocode

import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.infra.kakao.KakaoGeocodingClient
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Geocode", description = "역지오코딩 API")
@RestController
@RequestMapping("/api/v1/geocode")
class ReverseGeocodeController(private val kakaoGeocodingClient: KakaoGeocodingClient) {

    @Operation(summary = "좌표 → 주소 변환 (인증 불필요)")
    @GetMapping("/reverse")
    fun reverseGeocode(
        @RequestParam lat: Double,
        @RequestParam lng: Double,
    ): ApiResponse<Map<String, String>> {
        val address = kakaoGeocodingClient.reverseGeocode(lat, lng)
        return ApiResponse.ok(mapOf("address" to address))
    }
}
