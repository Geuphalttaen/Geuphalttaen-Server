package com.geuphalttaen.domain.user

import com.geuphalttaen.core.entity.ToiletStatus

data class UserProfileResponse(
    val nickname: String,
    val provider: String,
    val reportCount: Int,
    val postedCount: Int,
)

data class MyReportResponse(
    val id: Long,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val status: ToiletStatus,
    val createdAt: String,
)
