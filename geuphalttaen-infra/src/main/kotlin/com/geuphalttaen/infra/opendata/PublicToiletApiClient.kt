package com.geuphalttaen.infra.opendata

import com.fasterxml.jackson.annotation.JsonProperty
import com.geuphalttaen.domain.sync.SyncToiletDto
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
@EnableConfigurationProperties(OpendataProperties::class)
class PublicToiletApiClient(
    private val properties: OpendataProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient by lazy {
        RestClient.builder()
            .baseUrl(properties.baseUrl)
            .build()
    }

    fun fetchToilets(page: Int = 1, perPage: Int = properties.pageSize): ToiletApiResponse {
        val response = restClient.get()
            .uri { builder ->
                builder
                    .path("/15012893/v1/uddi:35baf9e8-44b6-4a98-9cb3-d94c8c9e5f24")
                    .queryParam("serviceKey", properties.apiKey)
                    .queryParam("page", page)
                    .queryParam("perPage", perPage)
                    .build()
            }
            .retrieve()
            .body(object : ParameterizedTypeReference<ToiletApiResponse>() {})
            ?: ToiletApiResponse(currentCount = 0, data = emptyList(), matchCount = 0, page = page, perPage = perPage, totalCount = 0)

        log.debug("공공데이터 화장실 조회: page={}, totalCount={}, received={}", page, response.totalCount, response.data.size)
        return response
    }

    fun fetchAllToilets(): List<SyncToiletDto> {
        val result = mutableListOf<SyncToiletDto>()
        var page = 1

        do {
            val response = fetchToilets(page = page)
            val dtos = response.data.map { it.toDto() }
            result.addAll(dtos)
            log.info("공공데이터 화장실 조회 page={}/{}, 건수={}", page, (response.totalCount / response.perPage) + 1, dtos.size)
            page++
        } while (result.size < response.totalCount)

        log.info("공공데이터 화장실 전체 조회 완료: 총 {}건", result.size)
        return result
    }
}

data class ToiletApiResponse(
    val currentCount: Int,
    val data: List<ToiletApiItem>,
    val matchCount: Int,
    val page: Int,
    val perPage: Int,
    val totalCount: Int,
)

data class ToiletApiItem(
    @JsonProperty("화장실명") val name: String?,
    @JsonProperty("도로명주소") val roadAddress: String?,
    @JsonProperty("지번주소") val lotAddress: String?,
    @JsonProperty("위도") val lat: String?,
    @JsonProperty("경도") val lng: String?,
    @JsonProperty("남성용-대변기수") val maleToiletCount: String?,
    @JsonProperty("여성용-대변기수") val femaleToiletCount: String?,
    @JsonProperty("장애인용-남성대변기수") val disabledMaleCount: String?,
    @JsonProperty("장애인용-여성대변기수") val disabledFemaleCount: String?,
    @JsonProperty("어린이용-남성대변기수") val familyMaleCount: String?,
    @JsonProperty("어린이용-여성대변기수") val familyFemaleCount: String?,
) {
    fun toDto(): SyncToiletDto {
        val resolvedAddress = roadAddress?.takeIf { it.isNotBlank() }
            ?: lotAddress?.takeIf { it.isNotBlank() }
            ?: ""
        val latVal = lat?.toDoubleOrNull() ?: 0.0
        val lngVal = lng?.toDoubleOrNull() ?: 0.0
        val hasMale = (maleToiletCount?.toIntOrNull() ?: 0) > 0
        val hasFemale = (femaleToiletCount?.toIntOrNull() ?: 0) > 0
        val hasDisabled = (disabledMaleCount?.toIntOrNull() ?: 0) > 0 ||
            (disabledFemaleCount?.toIntOrNull() ?: 0) > 0
        val hasFamilyRoom = (familyMaleCount?.toIntOrNull() ?: 0) > 0 ||
            (familyFemaleCount?.toIntOrNull() ?: 0) > 0

        return SyncToiletDto(
            name = name?.trim() ?: "",
            address = resolvedAddress.trim(),
            lat = latVal,
            lng = lngVal,
            male = hasMale,
            female = hasFemale,
            disabled = hasDisabled,
            familyRoom = hasFamilyRoom,
        )
    }
}
