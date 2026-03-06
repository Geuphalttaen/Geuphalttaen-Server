package com.geuphalttaen.server.application.service

import com.geuphalttaen.server.application.port.`in`.ToiletUseCase
import com.geuphalttaen.server.application.port.out.ToiletRepository
import com.geuphalttaen.server.common.exception.NotFoundException
import com.geuphalttaen.server.domain.Toilet
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ToiletService(
    private val toiletRepository: ToiletRepository,
) : ToiletUseCase {

    override fun findNearbyToilets(latitude: Double, longitude: Double, radiusKm: Double): List<Toilet> {
        return toiletRepository.findNearby(latitude, longitude, radiusKm)
    }

    override fun findById(id: Long): Toilet {
        return toiletRepository.findById(id) ?: throw NotFoundException("화장실 정보를 찾을 수 없습니다.")
    }
}
