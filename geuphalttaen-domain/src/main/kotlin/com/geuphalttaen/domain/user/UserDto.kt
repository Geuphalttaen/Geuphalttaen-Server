package com.geuphalttaen.domain.user

import com.geuphalttaen.core.entity.ToiletStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserProfileResponse(
    val nickname: String,
    val provider: String,
    val reportCount: Int,
    val postedCount: Int,
)

data class UpdateNicknameRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 20)
    val nickname: String,
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
