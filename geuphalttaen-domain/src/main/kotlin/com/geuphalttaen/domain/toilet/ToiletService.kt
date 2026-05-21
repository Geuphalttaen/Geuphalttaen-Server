package com.geuphalttaen.domain.toilet

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletImageEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.image.ImageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.*

@Service
class ToiletService(
    private val toiletRepository: ToiletRepository,
    private val imageService: ImageService,
) {
    fun searchNearby(request: ToiletSearchRequest): List<ToiletResponse> {
        val entities = toiletRepository.findNearby(request.lat, request.lng, request.radiusMeters)
        val toiletIds = entities.map { it.id }
        val imageMap = toiletRepository.findImagesByToiletIds(toiletIds)
            .groupBy({ it.toiletId }, { it.url })
        return entities.map { it.toResponse(request.lat, request.lng, imageMap[it.id] ?: emptyList()) }
    }

    fun getById(id: Long): ToiletResponse {
        val entity = toiletRepository.findById(id) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        val images = toiletRepository.findImagesByToiletId(id).map { it.url }
        return entity.toResponse(imageUrls = images)
    }

    @Transactional
    fun report(userId: Long, request: ToiletReportRequest): ToiletResponse {
        if (request.images.isNotEmpty()) {
            imageService.validateUrls(request.images.map { it.url })
            imageService.validateUrls(request.images.map { it.originalUrl })
        }

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

        val imageUrls = if (request.images.isNotEmpty()) {
            val imageEntities = request.images.map {
                ToiletImageEntity(toiletId = saved.id, url = it.url, originalUrl = it.originalUrl)
            }
            toiletRepository.saveImages(imageEntities).map { it.url }
        } else emptyList()

        return saved.toResponse(request.lat, request.lng, imageUrls)
    }

    private fun ToiletEntity.toResponse(
        fromLat: Double? = null,
        fromLng: Double? = null,
        imageUrls: List<String> = emptyList(),
    ): ToiletResponse =
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
            imageUrls = imageUrls,
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
