package com.geuphalttaen.infra.ncp

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ncp.maps")
data class NcpProperties(
    val clientId: String = "",
    val clientSecret: String = "",
)
