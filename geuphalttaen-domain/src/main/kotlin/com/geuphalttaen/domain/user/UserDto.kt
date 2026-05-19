package com.geuphalttaen.domain.user

data class UserProfileResponse(
    val id: Long,
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
    val status: String,
    val createdAt: String,
)
