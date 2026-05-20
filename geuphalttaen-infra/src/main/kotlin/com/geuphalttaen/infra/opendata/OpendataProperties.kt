package com.geuphalttaen.infra.opendata

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "opendata.toilet")
data class OpendataProperties(
    val csvUrl: String = "https://file.localdata.go.kr/file/download/public_restroom_info/info",
)
