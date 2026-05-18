package com.geuphalttaen.infra.opendata

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "opendata.toilet")
data class OpendataProperties(
    val apiKey: String = "",
    val baseUrl: String = "https://api.odcloud.kr/api",
)
