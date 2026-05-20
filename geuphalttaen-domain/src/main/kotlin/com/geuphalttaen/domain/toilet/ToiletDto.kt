package com.geuphalttaen.domain.toilet

import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import java.time.LocalDateTime

data class ToiletSearchRequest(
    val lat: Double,
    val lng: Double,
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
)

/**
 * 관리자용 화장실 수정 요청 DTO.
 * null인 필드는 수정하지 않는다.
 */
data class AdminToiletUpdateRequest(
    val name: String? = null,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val isPublic: Boolean? = null,
    val male: Boolean? = null,
    val female: Boolean? = null,
    val disabled: Boolean? = null,
    val familyRoom: Boolean? = null,
)

/**
 * ToiletEntity → AdminToiletResponse 변환 확장 함수.
 */
fun ToiletEntity.toAdminResponse(): AdminToiletResponse = AdminToiletResponse(
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
)
