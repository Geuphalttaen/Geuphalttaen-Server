package com.geuphalttaen.infra.oauth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.domain.auth.OAuthClientPort
import com.geuphalttaen.domain.auth.OAuthUserInfo
import org.slf4j.LoggerFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient

/**
 * 카카오 OAuth 클라이언트.
 * 카카오 사용자 정보 API를 호출하여 사용자 정보를 조회한다.
 */
@Component("KAKAO")
open class KakaoOAuthClient(
    private val kakaoProperties: KakaoProperties,
) : OAuthClientPort {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient by lazy { buildRestClient() }

    /** 테스트에서 오버라이드하여 MockRestServiceServer를 바인딩할 수 있도록 open으로 노출 */
    open fun buildRestClient(): RestClient {
        val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val converter = MappingJackson2HttpMessageConverter(objectMapper)
        return RestClient.builder()
            .messageConverters { converters ->
                converters.clear()
                converters.add(converter)
            }
            .build()
    }

    override fun getUserInfo(accessToken: String): OAuthUserInfo {
        val kakaoUserInfo = try {
            restClient.get()
                .uri(kakaoProperties.userInfoUrl)
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(KakaoUserInfoResponse::class.java)
                ?: throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)
        } catch (e: HttpClientErrorException) {
            log.warn("카카오 사용자 정보 조회 실패: status={}", e.statusCode)
            throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)
        } catch (e: HttpServerErrorException) {
            log.warn("카카오 서버 오류: status={}", e.statusCode)
            throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)
        } catch (e: ResourceAccessException) {
            log.warn("카카오 서버 연결 실패: {}", e.message)
            throw BusinessException(ErrorCode.OAUTH_INVALID_TOKEN)
        }

        return OAuthUserInfo(
            providerId = kakaoUserInfo.id.toString(),
            nickname = kakaoUserInfo.kakaoAccount?.profile?.nickname,
        )
    }
}

data class KakaoUserInfoResponse(
    val id: Long,
    @JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount?,
)

data class KakaoAccount(
    val profile: KakaoProfile?,
)

data class KakaoProfile(
    val nickname: String?,
)
