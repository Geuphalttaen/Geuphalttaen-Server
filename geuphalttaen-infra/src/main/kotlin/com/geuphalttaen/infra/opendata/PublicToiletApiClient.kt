package com.geuphalttaen.infra.opendata

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 공공데이터포털 화장실 정보 API 클라이언트.
 * TODO: 실제 API 스펙에 맞춰 구현 필요
 * API 문서: https://www.data.go.kr/data/15012893/standard.do
 */
@Component
class PublicToiletApiClient(
    private val opendataProperties: OpendataProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient by lazy {
        RestClient.builder()
            .baseUrl(opendataProperties.baseUrl)
            .build()
    }

    /**
     * TODO: 공공데이터 전국 화장실 정보를 페이지 단위로 조회
     */
    fun fetchToilets(page: Int = 1, perPage: Int = 100): List<PublicToiletDto> {
        log.warn("PublicToiletApiClient.fetchToilets is not yet implemented")
        // TODO: implement actual REST call
        return emptyList()
    }
}

data class PublicToiletDto(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val male: Boolean,
    val female: Boolean,
    val disabled: Boolean,
)
