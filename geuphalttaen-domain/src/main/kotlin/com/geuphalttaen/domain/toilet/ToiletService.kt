package com.geuphalttaen.domain.toilet

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletImageEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.image.ImageService
import com.geuphalttaen.domain.review.CleanlinessRepository
import com.geuphalttaen.domain.review.ReviewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.*

@Service
class ToiletService(
    private val toiletRepository: ToiletRepository,
    private val imageService: ImageService,
    private val reviewRepository: ReviewRepository,
    private val cleanlinessRepository: CleanlinessRepository,
) {
    fun searchNearby(request: ToiletSearchRequest): List<ToiletResponse> {
        val entities = toiletRepository.findNearby(request.lat, request.lng, request.radiusMeters)
        val toiletIds = entities.map { it.id }
        val imageMap = toiletRepository.findImagesByToiletIds(toiletIds)
            .groupBy({ it.toiletId }, { imageService.toPublicUrl(it.url) })
        val reviewStatsMap = if (toiletIds.isNotEmpty()) reviewRepository.findStatsByToiletIds(toiletIds) else emptyMap()
        val cleanlinessMap = if (toiletIds.isNotEmpty()) cleanlinessRepository.findAveragesByToiletIds(toiletIds) else emptyMap()
        return entities.map {
            val stats = reviewStatsMap[it.id]
            it.toResponse(
                fromLat = request.lat,
                fromLng = request.lng,
                imageUrls = imageMap[it.id] ?: emptyList(),
                averageRating = stats?.averageRating,
                reviewCount = stats?.reviewCount ?: 0L,
                averageCleanliness = cleanlinessMap[it.id],
            )
        }
    }

    fun getById(id: Long): ToiletResponse {
        val entity = toiletRepository.findById(id) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        val images = toiletRepository.findImagesByToiletId(id).map { imageService.toPublicUrl(it.url) }
        val stats = reviewRepository.findStatsByToiletId(id)
        val avgCleanliness = cleanlinessRepository.findAverageByToiletId(id)
        return entity.toResponse(
            imageUrls = images,
            averageRating = stats.averageRating,
            reviewCount = stats.reviewCount,
            averageCleanliness = avgCleanliness,
        )
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
            toiletRepository.saveImages(imageEntities).map { imageService.toPublicUrl(it.url) }
        } else emptyList()

        // 신규 제보 화장실은 리뷰/청결도 데이터 없음
        return saved.toResponse(request.lat, request.lng, imageUrls)
    }

    private fun ToiletEntity.toResponse(
        fromLat: Double? = null,
        fromLng: Double? = null,
        imageUrls: List<String> = emptyList(),
        averageRating: Double? = null,
        reviewCount: Long = 0L,
        averageCleanliness: Double? = null,
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
            averageRating = averageRating,
            reviewCount = reviewCount,
            averageCleanliness = averageCleanliness,
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
