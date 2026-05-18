package com.geuphalttaen.infra.oauth

import com.fasterxml.jackson.annotation.JsonProperty
import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.domain.auth.OAuthClientPort
import com.geuphalttaen.domain.auth.OAuthUserInfo
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Apple OAuth 클라이언트.
 * Apple identity token(JWT)을 검증하고 사용자 정보를 추출한다.
 * Apple 공개키는 1시간 단위로 인메모리 캐시한다.
 */
@Component("APPLE")
class AppleOAuthClient : OAuthClientPort {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient = RestClient.builder()
        .baseUrl("https://appleid.apple.com")
        .build()

    // 인메모리 캐시: kid → PublicKey, expiresAt
    private val keyCache = ConcurrentHashMap<String, CachedPublicKey>()
    private val cacheDurationSeconds = 3600L // 1시간

    override fun getUserInfo(accessToken: String): OAuthUserInfo {
        return try {
            val header = parseJwtHeader(accessToken)
            val kid = header["kid"] as? String
                ?: throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)

            val publicKey = getPublicKey(kid)

            val claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(accessToken)
                .payload

            val sub = claims.subject
                ?: throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)
            val email = claims["email"] as? String

            OAuthUserInfo(
                providerId = sub,
                nickname = email?.substringBefore("@"),
            )
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            log.warn("Apple identity token 검증 실패: {}", e.message)
            throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)
        }
    }

    private fun getPublicKey(kid: String): PublicKey {
        val cached = keyCache[kid]
        if (cached != null && Instant.now().epochSecond < cached.expiresAt) {
            return cached.publicKey
        }

        // Apple 공개키 목록 조회
        val keysResponse = restClient.get()
            .uri("/auth/keys")
            .retrieve()
            .body(ApplePublicKeysResponse::class.java)
            ?: throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)

        val keyInfo = keysResponse.keys.find { it.kid == kid }
            ?: throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)

        val publicKey = buildPublicKey(keyInfo)
        val expiresAt = Instant.now().epochSecond + cacheDurationSeconds
        keyCache[kid] = CachedPublicKey(publicKey, expiresAt)
        return publicKey
    }

    private fun buildPublicKey(keyInfo: ApplePublicKey): PublicKey {
        val decoder = Base64.getUrlDecoder()
        val modulus = BigInteger(1, decoder.decode(keyInfo.n))
        val exponent = BigInteger(1, decoder.decode(keyInfo.e))
        val keySpec = RSAPublicKeySpec(modulus, exponent)
        return KeyFactory.getInstance(keyInfo.kty).generatePublic(keySpec)
    }

    /**
     * JWT 헤더를 Base64 디코딩하여 Map으로 반환한다 (서명 검증 전 kid 추출용).
     */
    private fun parseJwtHeader(token: String): Map<String, Any> {
        val headerEncoded = token.split(".").firstOrNull()
            ?: throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)
        val headerJson = String(Base64.getUrlDecoder().decode(headerEncoded), Charsets.UTF_8)
        @Suppress("UNCHECKED_CAST")
        return com.fasterxml.jackson.databind.ObjectMapper().readValue(headerJson, Map::class.java) as Map<String, Any>
    }
}

data class CachedPublicKey(
    val publicKey: PublicKey,
    val expiresAt: Long,
)

data class ApplePublicKeysResponse(
    val keys: List<ApplePublicKey>,
)

data class ApplePublicKey(
    val kty: String,
    val kid: String,
    val use: String,
    val alg: String,
    val n: String,
    val e: String,
)
