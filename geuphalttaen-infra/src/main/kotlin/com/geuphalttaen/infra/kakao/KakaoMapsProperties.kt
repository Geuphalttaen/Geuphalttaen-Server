package com.geuphalttaen.infra.kakao

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kakao.maps")
data class KakaoMapsProperties(val restKey: String = "")
