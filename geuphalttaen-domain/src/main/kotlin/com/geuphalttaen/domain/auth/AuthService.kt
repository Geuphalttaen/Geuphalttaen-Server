package com.geuphalttaen.domain.auth

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.OAuthProvider
import com.geuphalttaen.core.entity.UserEntity
import org.springframework.stereotype.Service

/**
 * 인증 도메인 서비스.
 * OAuth 로그인, 토큰 갱신, 로그아웃을 처리한다.
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val oauthClients: Map<String, OAuthClientPort>,
) {
    /**
     * OAuth 로그인 처리.
     * 제공자로부터 사용자 정보를 조회하고, 신규 사용자이면 등록 후 JWT를 발급한다.
     */
    fun login(request: OAuthLoginRequest): TokenResponse {
        val oauthClient = oauthClients[request.provider.uppercase()]
            ?: throw BusinessException(ErrorCode.UNSUPPORTED_PROVIDER)

        val oauthUser = oauthClient.getUserInfo(request.accessToken)

        val provider = OAuthProvider.valueOf(request.provider.uppercase())
        val user = userRepository.findByProviderAndProviderId(request.provider, oauthUser.providerId)
            ?: userRepository.save(
                UserEntity(
                    provider = provider,
                    providerId = oauthUser.providerId,
                    nickname = oauthUser.nickname ?: "사용자",
                ),
            )

        val accessToken = jwtProvider.generateAccessToken(user.id)
        val refreshToken = jwtProvider.generateRefreshToken(user.id)
        refreshTokenRepository.save(user.id, refreshToken)
        return TokenResponse(accessToken, refreshToken)
    }

    /**
     * Refresh Token을 검증하고 새로운 토큰 쌍을 발급한다.
     */
    fun refresh(refreshToken: String): TokenResponse {
        if (!jwtProvider.isValid(refreshToken)) throw BusinessException(ErrorCode.INVALID_TOKEN)
        val userId = jwtProvider.getUserId(refreshToken)
        val stored = refreshTokenRepository.find(userId) ?: throw BusinessException(ErrorCode.TOKEN_NOT_FOUND)
        if (stored != refreshToken) throw BusinessException(ErrorCode.INVALID_TOKEN)
        val newAccessToken = jwtProvider.generateAccessToken(userId)
        val newRefreshToken = jwtProvider.generateRefreshToken(userId)
        refreshTokenRepository.save(userId, newRefreshToken)
        return TokenResponse(newAccessToken, newRefreshToken)
    }

    /**
     * 로그아웃: 저장된 Refresh Token을 삭제한다.
     */
    fun logout(userId: Long) {
        refreshTokenRepository.delete(userId)
    }
}
