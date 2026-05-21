package com.geuphalttaen.infra.r2

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cloudflare.r2")
data class CloudflareR2Properties(
    val accountId: String = "",
    val accessKeyId: String = "",
    val secretAccessKey: String = "",
    val bucketName: String = "",
    val publicUrl: String = "",
)
