package com.geuphalttaen.domain.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

class JwtProviderTest {

    private lateinit var jwtProvider: JwtProvider

    // 테스트용 시크릿: 최소 32자 이상 (HMAC-SHA256 요구사항)
    private val testSecret = "test-secret-key-must-be-at-least-32-characters-long-for-hmac"

    @BeforeEach
    fun setUp() {
        val props = JwtProperties(secret = testSecret)
        jwtProvider = JwtProvider(props)
    }

    @Test
    fun `generateAccessToken - 유효한 JWT 토큰이 반환된다`() {
        val userId = 42L

        val token = jwtProvider.generateAccessToken(userId)

        assertThat(token).isNotBlank()
    }

    @Test
    fun `getUserId - 토큰에서 userId를 추출한다`() {
        val userId = 42L
        val token = jwtProvider.generateAccessToken(userId)

        val extractedId = jwtProvider.getUserId(token)

        assertThat(extractedId).isEqualTo(userId)
    }

    @Test
    fun `isValid - 정상 토큰은 true를 반환한다`() {
        val token = jwtProvider.generateAccessToken(1L)

        assertThat(jwtProvider.isValid(token)).isTrue()
    }

    @Test
    fun `isValid - 변조된 토큰은 false를 반환한다`() {
        val token = jwtProvider.generateAccessToken(1L)
        val tampered = token.dropLast(5) + "XXXXX"

        assertThat(jwtProvider.isValid(tampered)).isFalse()
    }

    @Test
    fun `isValid - 빈 문자열은 false를 반환한다`() {
        assertThat(jwtProvider.isValid("")).isFalse()
    }

    @Test
    fun `generateRefreshToken - 정상 토큰이 반환되고 userId를 추출할 수 있다`() {
        val userId = 99L

        val token = jwtProvider.generateRefreshToken(userId)

        assertThat(jwtProvider.isValid(token)).isTrue()
        assertThat(jwtProvider.getUserId(token)).isEqualTo(userId)
    }

    @Test
    fun `getTokenType - Access Token은 ACCESS 타입을 반환한다`() {
        val token = jwtProvider.generateAccessToken(1L)

        assertThat(jwtProvider.getTokenType(token)).isEqualTo("ACCESS")
    }

    @Test
    fun `getTokenType - Refresh Token은 REFRESH 타입을 반환한다`() {
        val token = jwtProvider.generateRefreshToken(1L)

        assertThat(jwtProvider.getTokenType(token)).isEqualTo("REFRESH")
    }

    @Test
    fun `getTokenType - Admin Access Token은 ADMIN_ACCESS 타입을 반환한다`() {
        val token = jwtProvider.generateAdminAccessToken(1L)

        assertThat(jwtProvider.getTokenType(token)).isEqualTo("ADMIN_ACCESS")
    }

    @Test
    fun `getTokenType - 잘못된 토큰은 null을 반환한다`() {
        assertThat(jwtProvider.getTokenType("invalid.token")).isNull()
    }

    @Test
    fun `isValid - 만료된 토큰이면 false를 반환한다`() {
        // 이미 만료된 시각으로 expiration을 설정하여 만료 토큰 생성
        val key = Keys.hmacShaKeyFor(testSecret.toByteArray(Charsets.UTF_8))
        val now = Date()
        val expiredToken = Jwts.builder()
            .subject("1")
            .issuedAt(Date(now.time - 2_000L))
            .expiration(Date(now.time - 1_000L)) // 1초 전에 이미 만료
            .signWith(key)
            .compact()

        assertThat(jwtProvider.isValid(expiredToken)).isFalse()
    }
}
