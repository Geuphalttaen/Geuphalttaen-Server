package com.geuphalttaen.server.adapter.`in`.web.dto

import com.geuphalttaen.server.domain.Toilet
import java.time.LocalTime

data class ToiletResponse(
    val id: Long,
    val name: String,
    val roadAddress: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val maleToiletCount: Int,
    val femaleToiletCount: Int,
    val disabledMaleToiletCount: Int,
    val disabledFemaleToiletCount: Int,
    val openTime: LocalTime?,
    val closeTime: LocalTime?,
    val isDisabledAvailable: Boolean,
    val managementAgency: String?,
    val phoneNumber: String?,
) {
    companion object {
        fun from(toilet: Toilet) = ToiletResponse(
            id = toilet.id,
            name = toilet.name,
            roadAddress = toilet.roadAddress,
            address = toilet.address,
            latitude = toilet.latitude,
            longitude = toilet.longitude,
            maleToiletCount = toilet.maleToiletCount,
            femaleToiletCount = toilet.femaleToiletCount,
            disabledMaleToiletCount = toilet.disabledMaleToiletCount,
            disabledFemaleToiletCount = toilet.disabledFemaleToiletCount,
            openTime = toilet.openTime,
            closeTime = toilet.closeTime,
            isDisabledAvailable = toilet.isDisabledAvailable,
            managementAgency = toilet.managementAgency,
            phoneNumber = toilet.phoneNumber,
        )
    }
}
