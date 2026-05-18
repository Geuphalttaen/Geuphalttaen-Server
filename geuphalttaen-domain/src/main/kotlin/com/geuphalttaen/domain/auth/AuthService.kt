package com.geuphalttaen.domain.auth

import org.springframework.stereotype.Service

@Service
class AuthService(
    private val jwtProvider: JwtProvider,
) {
    /**
     * OAuth 로그인 처리.
     * TODO: provider별 사용자 정보 조회 (OAuthClient) 및 UserEntity upsert 구현
     */
    fun login(request: OAuthLoginRequest): TokenResponse {
        // TODO: Verify accessToken with OAuth provider and retrieve user info
        // TODO: Upsert UserEntity in DB
        // Stub: generate tokens for user id 0
        val userId = 0L
        return TokenResponse(
            accessToken = jwtProvider.generateAccessToken(userId),
            refreshToken = jwtProvider.generateRefreshToken(userId),
        )
    }
}
