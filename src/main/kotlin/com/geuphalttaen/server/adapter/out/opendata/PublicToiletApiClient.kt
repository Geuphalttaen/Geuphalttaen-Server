package com.geuphalttaen.server.adapter.out.opendata

import com.geuphalttaen.server.adapter.out.opendata.dto.PublicToiletApiResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class PublicToiletApiClient(
    @Value("\${opendata.api-key}")
    private val apiKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    companion object {
        private const val BASE_URL = "http://api.data.go.kr/openapi/tn_pubr_public_toilet_api"
        private const val PAGE_SIZE = 1000
    }

    fun fetchAll(): List<PublicToiletApiResponse.Item> {
        val allItems = mutableListOf<PublicToiletApiResponse.Item>()
        var pageNo = 1

        while (true) {
            val uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("serviceKey", apiKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", PAGE_SIZE)
                .queryParam("type", "json")
                .build(false)
                .toUriString()

            try {
                val response = restTemplate.getForObject(uri, PublicToiletApiResponse::class.java)
                val items = response?.response?.body?.items ?: break

                if (items.isEmpty()) break

                allItems.addAll(items)
                log.info("공공화장실 데이터 수집 중... page=$pageNo, 누적=${allItems.size}")

                if (items.size < PAGE_SIZE) break
                pageNo++
            } catch (e: Exception) {
                log.error("공공화장실 데이터 수집 실패 page=$pageNo", e)
                break
            }
        }

        log.info("공공화장실 데이터 수집 완료. 총 ${allItems.size}건")
        return allItems
    }
}
