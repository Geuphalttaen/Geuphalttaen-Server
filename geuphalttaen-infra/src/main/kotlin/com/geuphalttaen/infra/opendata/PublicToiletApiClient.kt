package com.geuphalttaen.infra.opendata

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.geuphalttaen.domain.sync.ExternalToiletData
import com.geuphalttaen.domain.sync.ToiletDataPort
import com.geuphalttaen.domain.sync.ToiletFetchResult
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * 공공데이터포털 전국 화장실 정보 API 클라이언트.
 * API: https://www.data.go.kr/data/15012893/standard.do
 */
@Component
open class PublicToiletApiClient(
    private val properties: OpendataProperties,
) : ToiletDataPort {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val API_PATH = "/15012893/v1/uddi:fc5bc6b6-88f1-49c9-b9cb-e8f5bdfd31b4"
        private const val MAX_RETRY = 3
        private const val PER_PAGE = 1000
    }

    private val restClient: RestClient by lazy { buildRestClient() }

    /** 테스트에서 오버라이드 가능하도록 open으로 노출 */
    open fun buildRestClient(): RestClient {
        val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val converter = MappingJackson2HttpMessageConverter(objectMapper)
        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .messageConverters { converters ->
                converters.clear()
                converters.add(converter)
            }
            .build()
    }

    /**
     * 단일 페이지 조회 (최대 MAX_RETRY 회 재시도).
     * QA#3: 모든 재시도 실패 시 RuntimeException 을 던진다.
     */
    fun fetchPage(page: Int, perPage: Int = PER_PAGE): ToiletApiResponse {
        var lastException: Exception? = null
        repeat(MAX_RETRY) { attempt ->
            try {
                val response = restClient.get()
                    .uri { builder ->
                        builder
                            .path(API_PATH)
                            .queryParam("page", page)
                            .queryParam("perPage", perPage)
                            .queryParam("serviceKey", properties.apiKey)
                            .build()
                    }
                    .retrieve()
                    .body(object : ParameterizedTypeReference<ToiletApiResponse>() {})
                    ?: ToiletApiResponse(currentCount = 0, data = emptyList(), matchCount = 0, page = page, perPage = perPage, totalCount = 0)
                log.debug("공공데이터 화장실 조회: page={}, totalCount={}, received={}", page, response.totalCount, response.data.size)
                return response
            } catch (e: RestClientException) {
                lastException = e
                log.warn("공공데이터 화장실 조회 실패 (시도 {}/{}): page={}, error={}", attempt + 1, MAX_RETRY, page, e.message)
            }
        }
        // QA#3: 최대 재시도 초과 시 예외를 던져 syncAll 이 FAILED 로그를 남기도록 한다
        val message = "공공데이터 화장실 조회 최대 재시도 초과: page=$page"
        log.error(message, lastException)
        throw RuntimeException(message, lastException)
    }

    /**
     * 전체 페이지를 순회하여 모든 화장실 데이터를 반환한다.
     * QA#3: API 호출 실패 시 RuntimeException 을 던진다 — syncAll 이 FAILED 로그를 기록한다.
     * I7: lat/lng 파싱 실패 건수를 ToiletFetchResult.parseFailCount 에 포함한다.
     */
    override fun fetchAllToilets(): ToiletFetchResult {
        val items = mutableListOf<ExternalToiletData>()
        var parseFailCount = 0
        var page = 1
        var totalCount = 0

        do {
            val response = fetchPage(page = page, perPage = PER_PAGE)
            if (response.data.isEmpty()) break
            totalCount = response.totalCount
            for (apiItem in response.data) {
                val data = apiItem.toExternalData()
                if (data != null) {
                    items.add(data)
                } else {
                    parseFailCount++
                }
            }
            log.info(
                "공공데이터 화장실 조회 page={}, 이번 페이지={}건, 누적={}건, 전체={}건",
                page, response.data.size, items.size, totalCount,
            )
            page++
        } while (items.size + parseFailCount < totalCount)

        log.info("공공데이터 화장실 전체 조회 완료: 정상={}건, 파싱실패={}건", items.size, parseFailCount)
        return ToiletFetchResult(items = items, parseFailCount = parseFailCount)
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
    @JsonProperty("장애인용-남변기수") val disabledMaleCount: String?,
    @JsonProperty("장애인용-여변기수") val disabledFemaleCount: String?,
    @JsonProperty("영유아보육시설") val childcareCount: String?,
    @JsonProperty("수유실") val nursingRoomCount: String?,
) {
    /**
     * lat/lng 파싱 실패 시 null 반환하여 skip 처리.
     */
    fun toExternalData(): ExternalToiletData? {
        val resolvedAddress = roadAddress?.takeIf { it.isNotBlank() }
            ?: lotAddress?.takeIf { it.isNotBlank() }
            ?: ""
        val latVal = lat?.toDoubleOrNull() ?: return null
        val lngVal = lng?.toDoubleOrNull() ?: return null
        val hasMale = (maleToiletCount?.toIntOrNull() ?: 0) > 0
        val hasFemale = (femaleToiletCount?.toIntOrNull() ?: 0) > 0
        val hasDisabled = (disabledMaleCount?.toIntOrNull() ?: 0) > 0 ||
            (disabledFemaleCount?.toIntOrNull() ?: 0) > 0
        val hasFamilyRoom = (childcareCount?.toIntOrNull() ?: 0) > 0 ||
            (nursingRoomCount?.toIntOrNull() ?: 0) > 0

        return ExternalToiletData(
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
