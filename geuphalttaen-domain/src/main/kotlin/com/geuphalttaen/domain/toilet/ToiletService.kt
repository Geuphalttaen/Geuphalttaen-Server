package com.geuphalttaen.domain.toilet

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import org.springframework.stereotype.Service
import kotlin.math.*

@Service
class ToiletService(
    private val toiletRepository: ToiletRepository,
) {
    fun searchNearby(request: ToiletSearchRequest): List<ToiletResponse> {
        val entities = toiletRepository.findNearby(request.lat, request.lng, request.radiusMeters)
        return entities.map { it.toResponse(request.lat, request.lng) }
    }

    fun getById(id: Long): ToiletResponse {
        val entity = toiletRepository.findById(id) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        return entity.toResponse()
    }

    fun report(userId: Long, request: ToiletReportRequest): ToiletResponse {
        val entity = ToiletEntity(
            name = request.name,
            address = request.address,
            lat = request.lat,
            lng = request.lng,
            isPublic = request.isPublic,
            male = request.male,
            female = request.female,
            disabled = request.disabled,
            familyRoom = request.familyRoom,
            reportedBy = userId,
            status = ToiletStatus.PENDING,
        )
        val saved = toiletRepository.save(entity)
        return saved.toResponse(request.lat, request.lng)
    }

    private fun ToiletEntity.toResponse(fromLat: Double? = null, fromLng: Double? = null): ToiletResponse =
        ToiletResponse(
            id = id,
            name = name,
            address = address,
            lat = lat,
            lng = lng,
            distanceMeters = if (fromLat != null && fromLng != null) haversineMeters(fromLat, fromLng, lat, lng) else null,
            male = male,
            female = female,
            disabled = disabled,
            familyRoom = familyRoom,
            isPublic = isPublic,
        )

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lng2 - lng1)
        val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
