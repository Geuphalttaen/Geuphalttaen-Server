package com.geuphalttaen.server.adapter.`in`.web

import com.geuphalttaen.server.adapter.`in`.web.dto.ToiletResponse
import com.geuphalttaen.server.application.port.`in`.ToiletUseCase
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/toilets")
class ToiletController(
    private val toiletUseCase: ToiletUseCase,
) {

    @GetMapping("/nearby")
    fun getNearbyToilets(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam(defaultValue = "1.0") radiusKm: Double,
    ): List<ToiletResponse> {
        return toiletUseCase.findNearbyToilets(latitude, longitude, radiusKm)
            .map { ToiletResponse.from(it) }
    }

    @GetMapping("/{id}")
    fun getToilet(@PathVariable id: Long): ToiletResponse {
        return ToiletResponse.from(toiletUseCase.findById(id))
    }
}
