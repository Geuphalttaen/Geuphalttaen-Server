package com.geuphalttaen.domain.auth

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.OAuthProvider
import com.geuphalttaen.core.entity.UserEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.ArgumentCaptor

/**
 * null-safe한 any() 헬퍼: Kotlin에서 Mockito any()가 null을 반환하는 문제를 우회한다.
 */
private fun <T> anyNonNull(type: Class<T>): T = org.mockito.ArgumentMatchers.any(type)

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    private lateinit var kakaoOAuthClient: OAuthClientPort

    private lateinit var jwtProvider: JwtProvider
    private lateinit var authService: AuthService

    private val testSecret = "test-secret-key-must-be-at-least-32-characters-long-for-hmac"

    @BeforeEach
    fun setUp() {
        jwtProvider = JwtProvider(JwtProperties(secret = testSecret))
        val oauthClients: Map<String, OAuthClientPort> = mapOf("KAKAO" to kakaoOAuthClient)
        authService = AuthService(userRepository, refreshTokenRepository, jwtProvider, oauthClients)
    }

    @Test
    fun `login - 신규 사용자는 저장 후 토큰을 반환한다`() {
        val request = OAuthLoginRequest(provider = "KAKAO", accessToken = "kakao-access-token")
        val oauthUser = OAuthUserInfo(providerId = "kakao-12345", nickname = "테스터")
        val savedUser = UserEntity(id = 1L, provider = OAuthProvider.KAKAO, providerId = "kakao-12345", nickname = "테스터")

        `when`(kakaoOAuthClient.getUserInfo("kakao-access-token")).thenReturn(oauthUser)
        `when`(userRepository.findByProviderAndProviderId("KAKAO", "kakao-12345")).thenReturn(null)

        val captor = ArgumentCaptor.forClass(UserEntity::class.java)
        `when`(userRepository.save(captor.capture() ?: UserEntity(provider = OAuthProvider.KAKAO, providerId = "", nickname = ""))).thenReturn(savedUser)

        val result = authService.login(request)

        assertThat(result.accessToken).isNotBlank()
        assertThat(result.refreshToken).isNotBlank()
        verify(refreshTokenRepository).save(eq(1L), anyString())
    }

    @Test
    fun `login - 기존 사용자는 저장 없이 토큰을 반환한다`() {
        val request = OAuthLoginRequest(provider = "KAKAO", accessToken = "kakao-access-token")
        val oauthUser = OAuthUserInfo(providerId = "kakao-12345", nickname = "테스터")
        val existingUser = UserEntity(id = 2L, provider = OAuthProvider.KAKAO, providerId = "kakao-12345", nickname = "테스터")

        `when`(kakaoOAuthClient.getUserInfo("kakao-access-token")).thenReturn(oauthUser)
        `when`(userRepository.findByProviderAndProviderId("KAKAO", "kakao-12345")).thenReturn(existingUser)

        val result = authService.login(request)

        assertThat(result.accessToken).isNotBlank()
        assertThat(result.refreshToken).isNotBlank()
        verify(userRepository, never()).save(anyNonNull(UserEntity::class.java))
        verify(refreshTokenRepository).save(eq(2L), anyString())
    }

    @Test
    fun `login - 지원하지 않는 provider는 UNSUPPORTED_PROVIDER 예외를 던진다`() {
        val request = OAuthLoginRequest(provider = "GOOGLE", accessToken = "google-access-token")

        assertThatThrownBy { authService.login(request) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.UNSUPPORTED_PROVIDER)
    }

    @Test
    fun `refresh - 유효한 Refresh Token으로 새 토큰 쌍을 반환한다`() {
        val userId = 5L
        val validRefreshToken = jwtProvider.generateRefreshToken(userId)

        `when`(refreshTokenRepository.find(userId)).thenReturn(validRefreshToken)

        val result = authService.refresh(validRefreshToken)

        assertThat(result.accessToken).isNotBlank()
        assertThat(result.refreshToken).isNotBlank()
        verify(refreshTokenRepository).save(eq(userId), anyString())
    }

    @Test
    fun `refresh - 유효하지 않은 토큰은 INVALID_TOKEN 예외를 던진다`() {
        val invalidToken = "invalid.token.value"

        assertThatThrownBy { authService.refresh(invalidToken) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.INVALID_TOKEN)
    }

    @Test
    fun `refresh - Redis에 토큰이 없으면 TOKEN_NOT_FOUND 예외를 던진다`() {
        val userId = 7L
        val validRefreshToken = jwtProvider.generateRefreshToken(userId)

        `when`(refreshTokenRepository.find(userId)).thenReturn(null)

        assertThatThrownBy { authService.refresh(validRefreshToken) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOKEN_NOT_FOUND)
    }

    @Test
    fun `logout - userId로 Refresh Token을 삭제한다`() {
        val userId = 10L

        authService.logout(userId)

        verify(refreshTokenRepository).delete(userId)
    }
}
