package com.geuphalttaen.server.adapter.out.opendata.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PublicToiletApiResponse(
    val response: Response,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Response(
        val body: Body,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Body(
        val items: List<Item>?,
        val totalCount: Int?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Item(
        val toiletNm: String?,           // 화장실명
        val rdnmadr: String?,            // 도로명주소
        val lnmadr: String?,             // 지번주소
        val latitude: String?,           // 위도
        val longitude: String?,          // 경도
        val menToiletBowlNumber: String?,    // 남성용 대변기 수
        val womenToiletBowlNumber: String?,  // 여성용 대변기 수
        val menHandicapToiletBowlNumber: String?,    // 남성용 장애인 대변기 수
        val womenHandicapToiletBowlNumber: String?,  // 여성용 장애인 대변기 수
        val openTime: String?,           // 개방시간
        val closeTime: String?,          // 폐쇄시간
        val institutionNm: String?,      // 관리기관명
        val phoneNumber: String?,        // 전화번호
    )
}
