package com.geuphalttaen.domain.admin

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 관리자 관련 설정 프로퍼티.
 * seedSecret: 최초 관리자 계정 생성 시 필요한 시크릿 헤더 값.
 */
@ConfigurationProperties(prefix = "admin")
data class AdminProperties(
    val seedSecret: String = "",
)
