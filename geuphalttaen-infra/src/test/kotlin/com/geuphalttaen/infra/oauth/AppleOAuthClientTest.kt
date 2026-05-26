package com.geuphalttaen.infra.oauth

import com.fasterxml.jackson.databind.ObjectMapper
import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import io.jsonwebtoken.Jwts
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date

/**
 * AppleOAuthClient 단위 테스트.
 * 테스트용 RSA 키 쌍을 생성하고 MockRestServiceServer로 Apple JWKS 엔드포인트를 목킹한다.
 */
class AppleOAuthClientTest {

    private lateinit var client: TestableAppleOAuthClient
    private lateinit var mockServer: MockRestServiceServer

    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val testKid = "test-key-id"

    @BeforeEach
    fun setUp() {
        val restClientBuilder = RestClient.builder().baseUrl("https://appleid.apple.com")
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        client = TestableAppleOAuthClient(restClientBuilder.build())
    }

    @Test
    fun `getUserInfo - 유효한 identityToken이면 OAuthUserInfo를 반환한다`() {
        val token = buildAppleToken(sub = "apple-user-123", email = "user@example.com")
        mockServer.expect(requestTo("https://appleid.apple.com/auth/keys"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(buildJwksResponse(), MediaType.APPLICATION_JSON))

        val result = client.getUserInfo(token)

        assertThat(result.providerId).isEqualTo("apple-user-123")
        assertThat(result.nickname).isEqualTo("user")
        mockServer.verify()
    }

    @Test
    fun `getUserInfo - 서명이 잘못된 토큰이면 OAUTH_INVALID_TOKEN 예외를 던진다`() {
        val wrongKeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val token = Jwts.builder()
            .subject("someone")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600_000))
            .header().add("kid", testKid).and()
            .signWith(wrongKeyPair.private)
            .compact()

        mockServer.expect(requestTo("https://appleid.apple.com/auth/keys"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(buildJwksResponse(), MediaType.APPLICATION_JSON))

        assertThatThrownBy { client.getUserInfo(token) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.OAUTH_INVALID_TOKEN)
    }

    @Test
    fun `getUserInfo - Apple JWKS 서버가 오류를 반환하면 OAUTH_INVALID_TOKEN 예외를 던진다`() {
        val token = buildAppleToken(sub = "apple-user-456", email = null)
        mockServer.expect(requestTo("https://appleid.apple.com/auth/keys"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        assertThatThrownBy { client.getUserInfo(token) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.OAUTH_INVALID_TOKEN)
    }

    @Test
    fun `getUserInfo - kid가 JWKS에 없으면 OAUTH_INVALID_TOKEN 예외를 던진다`() {
        val token = Jwts.builder()
            .subject("someone")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600_000))
            .header().add("kid", "unknown-kid").and()
            .signWith(keyPair.private)
            .compact()

        mockServer.expect(requestTo("https://appleid.apple.com/auth/keys"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(buildJwksResponse(), MediaType.APPLICATION_JSON))

        assertThatThrownBy { client.getUserInfo(token) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.OAUTH_INVALID_TOKEN)
    }

    private fun buildAppleToken(sub: String, email: String?): String {
        val builder = Jwts.builder()
            .subject(sub)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600_000))
            .header().add("kid", testKid).and()
            .signWith(keyPair.private)
        if (email != null) builder.claim("email", email)
        return builder.compact()
    }

    private fun buildJwksResponse(): String {
        val pub = keyPair.public as RSAPublicKey
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val n = encoder.encodeToString(pub.modulus.toByteArray().let {
            if (it[0] == 0.toByte()) it.drop(1).toByteArray() else it
        })
        val e = encoder.encodeToString(pub.publicExponent.toByteArray().let {
            if (it[0] == 0.toByte()) it.drop(1).toByteArray() else it
        })
        return ObjectMapper().writeValueAsString(
            mapOf("keys" to listOf(mapOf("kty" to "RSA", "kid" to testKid, "use" to "sig", "alg" to "RS256", "n" to n, "e" to e)))
        )
    }

    class TestableAppleOAuthClient(private val injectedRestClient: RestClient) : AppleOAuthClient() {
        override fun buildRestClient(): RestClient = injectedRestClient
    }
}
