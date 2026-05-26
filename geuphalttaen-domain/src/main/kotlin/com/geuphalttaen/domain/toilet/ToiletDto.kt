package com.geuphalttaen.domain.toilet

import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class ToiletSearchRequest(
    @field:DecimalMin("-90.0") @field:DecimalMax("90.0") val lat: Double,
    @field:DecimalMin("-180.0") @field:DecimalMax("180.0") val lng: Double,
    val radiusMeters: Int = 1000,
)

data class ToiletResponse(
    val id: Long,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val distanceMeters: Double?,
    val male: Boolean,
    val female: Boolean,
    val disabled: Boolean,
    val familyRoom: Boolean,
    val isPublic: Boolean,
    val imageUrls: List<String> = emptyList(),
    val averageRating: Double? = null,
    val reviewCount: Long = 0L,
    val averageCleanliness: Double? = null,
)

data class ImageRef(
    @field:NotBlank val url: String,
    @field:NotBlank val originalUrl: String,
)

data class ToiletReportRequest(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val isPublic: Boolean = true,
    val male: Boolean = true,
    val female: Boolean = true,
    val disabled: Boolean = false,
    val familyRoom: Boolean = false,
    @field:Valid
    @field:Size(max = 5, message = "이미지는 최대 5장까지 첨부할 수 있습니다.")
    val images: List<ImageRef> = emptyList(),
)

data class ImageUploadResponse(
    val url: String,
    val originalUrl: String,
)

/**
 * 관리자용 화장실 응답 DTO (status, reportedBy, createdAt 포함).
 */
data class AdminToiletResponse(
    val id: Long,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val isPublic: Boolean,
    val male: Boolean,
    val female: Boolean,
    val disabled: Boolean,
    val familyRoom: Boolean,
    val reportedBy: Long?,
    val status: ToiletStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val imageUrls: List<String> = emptyList(),
)

/**
 * 관리자용 화장실 수정 요청 DTO.
 * null인 필드는 수정하지 않는다.
 */
data class AdminToiletUpdateRequest(
    @field:Size(min = 1, max = 100) val name: String? = null,
    @field:Size(min = 1, max = 255) val address: String? = null,
    @field:DecimalMin("-90.0") @field:DecimalMax("90.0") val lat: Double? = null,
    @field:DecimalMin("-180.0") @field:DecimalMax("180.0") val lng: Double? = null,
    val isPublic: Boolean? = null,
    val male: Boolean? = null,
    val female: Boolean? = null,
    val disabled: Boolean? = null,
    val familyRoom: Boolean? = null,
)

/**
 * ToiletEntity → AdminToiletResponse 변환 확장 함수.
 */
fun ToiletEntity.toAdminResponse(imageUrls: List<String> = emptyList()): AdminToiletResponse = AdminToiletResponse(
    id = id,
    name = name,
    address = address,
    lat = lat,
    lng = lng,
    isPublic = isPublic,
    male = male,
    female = female,
    disabled = disabled,
    familyRoom = familyRoom,
    reportedBy = reportedBy,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    imageUrls = imageUrls,
)
