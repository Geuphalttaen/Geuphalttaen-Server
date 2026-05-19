package com.geuphalttaen.infra.oauth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kakao.oauth")
data class KakaoProperties(
    val clientId: String = "",
    val clientSecret: String = "",
    val tokenUrl: String = "https://kauth.kakao.com/oauth/token",
    val userInfoUrl: String = "https://kapi.kakao.com/v2/user/me",
)
