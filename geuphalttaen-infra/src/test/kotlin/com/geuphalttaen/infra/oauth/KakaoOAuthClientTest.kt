package com.geuphalttaen.infra.oauth

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

/**
 * KakaoOAuthClient 단위 테스트.
 * MockRestServiceServer를 이용해 HTTP 호출을 목킹한다.
 */
class KakaoOAuthClientTest {

    private val kakaoProperties = KakaoProperties(
        clientId = "test-client-id",
        clientSecret = "test-client-secret",
        userInfoUrl = "https://kapi.kakao.com/v2/user/me",
    )

    private lateinit var kakaoOAuthClient: TestableKakaoOAuthClient
    private lateinit var mockServer: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val converter = MappingJackson2HttpMessageConverter(objectMapper)

        val restClientBuilder = RestClient.builder()
            .messageConverters { converters ->
                converters.clear()
                converters.add(converter)
            }
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        kakaoOAuthClient = TestableKakaoOAuthClient(kakaoProperties, restClientBuilder.build())
    }

    @Test
    fun `getUserInfo - 정상 응답이면 OAuthUserInfo를 반환한다`() {
        val accessToken = "valid-kakao-token"
        val responseJson = """
            {
                "id": 987654321,
                "kakao_account": {
                    "profile": {
                        "nickname": "카카오유저"
                    }
                }
            }
        """.trimIndent()

        mockServer.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer $accessToken"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val result = kakaoOAuthClient.getUserInfo(accessToken)

        assertThat(result.providerId).isEqualTo("987654321")
        assertThat(result.nickname).isEqualTo("카카오유저")
        mockServer.verify()
    }

    @Test
    fun `getUserInfo - 401 응답이면 OAUTH_INVALID_TOKEN 예외를 던진다`() {
        val accessToken = "expired-token"

        mockServer.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED))

        assertThatThrownBy { kakaoOAuthClient.getUserInfo(accessToken) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.OAUTH_INVALID_TOKEN)

        mockServer.verify()
    }

    /**
     * 테스트용 KakaoOAuthClient: RestClient를 외부에서 주입받는다.
     */
    class TestableKakaoOAuthClient(
        kakaoProperties: KakaoProperties,
        private val injectedRestClient: RestClient,
    ) : KakaoOAuthClient(kakaoProperties) {
        override fun buildRestClient(): RestClient = injectedRestClient
    }
}
