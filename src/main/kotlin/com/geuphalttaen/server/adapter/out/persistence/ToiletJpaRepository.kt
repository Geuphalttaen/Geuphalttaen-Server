package com.geuphalttaen.server.adapter.out.persistence

import com.geuphalttaen.server.domain.Toilet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ToiletJpaRepository : JpaRepository<Toilet, Long> {

    @Query(
        """
        SELECT t FROM Toilet t
        WHERE (6371 * acos(
            cos(radians(:latitude)) * cos(radians(t.latitude)) *
            cos(radians(t.longitude) - radians(:longitude)) +
            sin(radians(:latitude)) * sin(radians(t.latitude))
        )) <= :radiusKm
        ORDER BY (6371 * acos(
            cos(radians(:latitude)) * cos(radians(t.latitude)) *
            cos(radians(t.longitude) - radians(:longitude)) +
            sin(radians(:latitude)) * sin(radians(t.latitude))
        )) ASC
        """
    )
    fun findNearby(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radiusKm") radiusKm: Double,
    ): List<Toilet>
}
