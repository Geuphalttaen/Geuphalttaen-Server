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

    fun generateAccessToken(userId: Long): String = buildToken(userId, accessTokenExpiryMs)

    fun generateRefreshToken(userId: Long): String = buildToken(userId, refreshTokenExpiryMs)

    fun getUserId(token: String): Long {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims.subject.toLong()
    }

    fun isValid(token: String): Boolean = runCatching { getUserId(token) }.isSuccess

    private fun buildToken(userId: Long, expiryMs: Long): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(Date(now.time + expiryMs))
            .signWith(key)
            .compact()
    }
}
