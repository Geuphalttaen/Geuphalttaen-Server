package com.geuphalttaen.server.adapter.out.persistence

import com.geuphalttaen.server.application.port.out.ToiletRepository
import com.geuphalttaen.server.domain.Toilet
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class ToiletRepositoryImpl(
    private val toiletJpaRepository: ToiletJpaRepository,
) : ToiletRepository {

    override fun findNearby(latitude: Double, longitude: Double, radiusKm: Double): List<Toilet> {
        return toiletJpaRepository.findNearby(latitude, longitude, radiusKm)
    }

    override fun findById(id: Long): Toilet? {
        return toiletJpaRepository.findByIdOrNull(id)
    }

    override fun saveAll(toilets: List<Toilet>): List<Toilet> {
        return toiletJpaRepository.saveAll(toilets)
    }

    override fun count(): Long {
        return toiletJpaRepository.count()
    }
}
