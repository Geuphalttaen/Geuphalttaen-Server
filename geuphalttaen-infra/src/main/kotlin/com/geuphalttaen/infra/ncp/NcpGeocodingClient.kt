package com.geuphalttaen.infra.ncp

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ReverseGeocodeResponse(val status: RgStatus?, val results: List<RgResult>?)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class RgStatus(val code: Int?)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class RgResult(val name: String?, val region: RgRegion?, val land: RgLand?)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class RgRegion(
    val area1: RgArea?,
    val area2: RgArea?,
    val area3: RgArea?,
    val area4: RgArea?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class RgArea(val name: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class RgLand(val name: String?, val number1: String?, val number2: String?)

@Component
class NcpGeocodingClient(private val ncpProperties: NcpProperties) {

    private val restClient = RestClient.builder()
        .baseUrl("https://naveropenapi.apigw.ntruss.com")
        .defaultHeader("X-NCP-APIGW-API-KEY-ID", ncpProperties.clientId)
        .defaultHeader("X-NCP-APIGW-API-KEY", ncpProperties.clientSecret)
        .build()

    fun reverseGeocode(lat: Double, lng: Double): String {
        val response = restClient.get()
            .uri("/map-reversegeocode/v2/gc?coords={c}&orders=roadaddr,addr&output=json", "$lng,$lat")
            .retrieve()
            .body<ReverseGeocodeResponse>()

        val result = response?.results?.firstOrNull() ?: return ""
        return formatAddress(result)
    }

    private fun formatAddress(result: RgResult): String {
        val region = result.region
        val parts = mutableListOf<String>()

        region?.area1?.name?.takeIf { it.isNotBlank() }?.let { parts += it }
        region?.area2?.name?.takeIf { it.isNotBlank() }?.let { parts += it }
        region?.area3?.name?.takeIf { it.isNotBlank() }?.let { parts += it }
        region?.area4?.name?.takeIf { it.isNotBlank() }?.let { parts += it }

        result.land?.let { land ->
            land.name?.takeIf { it.isNotBlank() }?.let { parts += it }
            val num = buildString {
                land.number1?.takeIf { it.isNotBlank() }?.let { append(it) }
                land.number2?.takeIf { it.isNotBlank() }?.let { append("-$it") }
            }
            if (num.isNotBlank()) parts += num
        }

        return parts.joinToString(" ")
    }
}
