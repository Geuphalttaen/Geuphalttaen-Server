package com.geuphalttaen.domain.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties,
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(Charsets.UTF_8))
    }

    private val accessTokenExpiryMs = 60 * 60 * 1_000L          // 1 hour
    private val refreshTokenExpiryMs = 14 * 24 * 60 * 60 * 1_000L // 14 days

    fun generateAccessToken(userId: Long): String = buildToken(userId, accessTokenExpiryMs, "ACCESS")

    fun generateRefreshToken(userId: Long): String = buildToken(userId, refreshTokenExpiryMs, "REFRESH")

    /**
     * 관리자용 Access Token 발급 (ROLE_ADMIN 권한 포함).
     */
    fun generateAdminAccessToken(adminId: Long): String = buildToken(adminId, accessTokenExpiryMs, "ACCESS", listOf("ROLE_ADMIN"))

    fun getUserId(token: String): Long {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims.subject.toLong()
    }

    fun isValid(token: String): Boolean = runCatching { getUserId(token) }.isSuccess

    fun getTokenType(token: String): String? = runCatching {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload["type"] as? String
    }.getOrNull()

    /**
     * 토큰에 포함된 역할(role) 목록을 반환한다.
     * 역할이 없으면 빈 리스트를 반환한다.
     */
    @Suppress("UNCHECKED_CAST")
    fun getRoles(token: String): List<String> = runCatching {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload["roles"] as? List<String>
    }.getOrNull() ?: emptyList()

    private fun buildToken(userId: Long, expiryMs: Long, type: String, roles: List<String> = emptyList()): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(Date(now.time + expiryMs))
            .claim("type", type)
            .apply { if (roles.isNotEmpty()) claim("roles", roles) }
            .signWith(key)
            .compact()
    }
}
