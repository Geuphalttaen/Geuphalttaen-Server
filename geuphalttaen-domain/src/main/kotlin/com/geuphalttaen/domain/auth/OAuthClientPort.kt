package com.geuphalttaen.domain.auth

/**
 * OAuth 제공자별 사용자 정보 조회 포트 (도메인 인터페이스).
 * 인프라 레이어의 각 OAuth 클라이언트가 구현한다.
 */
interface OAuthClientPort {
    fun getUserInfo(accessToken: String): OAuthUserInfo
}

data class OAuthUserInfo(
    val providerId: String,
    val nickname: String?,
)
