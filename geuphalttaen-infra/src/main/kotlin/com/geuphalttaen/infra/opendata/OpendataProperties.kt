package com.geuphalttaen.infra.opendata

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "opendata.toilet")
data class OpendataProperties(
    // TODO: PublicToiletApiClient.fetchToilets() 구현 시 API 호출에 사용 예정
    val apiKey: String = "",
    val baseUrl: String = "https://api.odcloud.kr/api",
)
