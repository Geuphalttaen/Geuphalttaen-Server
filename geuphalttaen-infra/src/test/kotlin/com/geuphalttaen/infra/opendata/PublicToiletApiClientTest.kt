package com.geuphalttaen.infra.opendata

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

/**
 * PublicToiletApiClient 단위 테스트.
 * MockRestServiceServer로 HTTP 호출을 목킹한다.
 */
class PublicToiletApiClientTest {

    private val baseUrl = "https://api.odcloud.kr/api"
    private val apiKey = "test-key"

    private lateinit var client: TestablePublicToiletApiClient
    private lateinit var mockServer: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val converter = MappingJackson2HttpMessageConverter(objectMapper)
        val builder = RestClient.builder()
            .baseUrl(baseUrl)
            .messageConverters { converters ->
                converters.clear()
                converters.add(converter)
            }
        mockServer = MockRestServiceServer.bindTo(builder).build()
        val properties = OpendataProperties(apiKey = apiKey, baseUrl = baseUrl, pageSize = 1000)
        client = TestablePublicToiletApiClient(properties, builder.build())
    }

    private fun pageResponse(totalCount: Int, page: Int, perPage: Int, vararg names: String): String {
        val items = names.joinToString(",") { name ->
            """{"화장실명":"$name","도로명주소":"서울시 테스트로 $name","위도":"37.5","경도":"127.0","남성용-대변기수":"2","여성용-대변기수":"2","장애인용-남변기수":"0","장애인용-여변기수":"0","영유아보육시설":"0","수유실":"0"}"""
        }
        return """{"currentCount":${names.size},"data":[$items],"matchCount":$totalCount,"page":$page,"perPage":$perPage,"totalCount":$totalCount}"""
    }

    @Test
    fun `fetchAllToilets - 2페이지 정상 처리`() {
        val page1 = pageResponse(totalCount = 2, page = 1, perPage = 1000, "화장실1")
        val page2 = pageResponse(totalCount = 2, page = 2, perPage = 1000, "화장실2")

        mockServer.expect(method(HttpMethod.GET))
            .andRespond(withSuccess(page1, MediaType.APPLICATION_JSON))
        mockServer.expect(method(HttpMethod.GET))
            .andRespond(withSuccess(page2, MediaType.APPLICATION_JSON))

        val result = client.fetchAllToilets()

        assertThat(result.items).hasSize(2)
        assertThat(result.items.map { it.name }).containsExactly("화장실1", "화장실2")
        assertThat(result.parseFailCount).isEqualTo(0)
        mockServer.verify()
    }

    @Test
    fun `fetchAllToilets - 빈 응답`() {
        val emptyResponse = """{"currentCount":0,"data":[],"matchCount":0,"page":1,"perPage":1000,"totalCount":0}"""
        mockServer.expect(method(HttpMethod.GET))
            .andRespond(withSuccess(emptyResponse, MediaType.APPLICATION_JSON))

        val result = client.fetchAllToilets()

        assertThat(result.items).isEmpty()
        mockServer.verify()
    }

    @Test
    fun `toExternalData - 한글 필드 파싱 정확성`() {
        val item = ToiletApiItem(
            name = "강남화장실",
            roadAddress = "서울시 강남구 테헤란로 1",
            lotAddress = null,
            lat = "37.5",
            lng = "127.0",
            maleToiletCount = "3",
            femaleToiletCount = "0",
            disabledMaleCount = "1",
            disabledFemaleCount = "0",
            childcareCount = "0",
            nursingRoomCount = "1",
        )
        val data = item.toExternalData()!!
        assertThat(data.name).isEqualTo("강남화장실")
        assertThat(data.address).isEqualTo("서울시 강남구 테헤란로 1")
        assertThat(data.lat).isEqualTo(37.5)
        assertThat(data.lng).isEqualTo(127.0)
        assertThat(data.male).isTrue()
        assertThat(data.female).isFalse()
        assertThat(data.disabled).isTrue()
        assertThat(data.familyRoom).isTrue()
    }

    @Test
    fun `toExternalData - 위도 파싱 실패 시 null 반환`() {
        val item = ToiletApiItem(
            name = "화장실",
            roadAddress = "주소",
            lotAddress = null,
            lat = "N/A",
            lng = "127.0",
            maleToiletCount = "1",
            femaleToiletCount = "1",
            disabledMaleCount = "0",
            disabledFemaleCount = "0",
            childcareCount = "0",
            nursingRoomCount = "0",
        )
        assertThat(item.toExternalData()).isNull()
    }

    @Test
    fun `toExternalData - 경도 파싱 실패 시 null 반환`() {
        val item = ToiletApiItem(
            name = "화장실",
            roadAddress = "주소",
            lotAddress = null,
            lat = "37.5",
            lng = "",
            maleToiletCount = "1",
            femaleToiletCount = "1",
            disabledMaleCount = "0",
            disabledFemaleCount = "0",
            childcareCount = "0",
            nursingRoomCount = "0",
        )
        assertThat(item.toExternalData()).isNull()
    }

    @Test
    fun `toExternalData - 도로명주소 없으면 지번주소 사용`() {
        val item = ToiletApiItem(
            name = "화장실",
            roadAddress = null,
            lotAddress = "서울시 강남구 역삼동 123",
            lat = "37.5",
            lng = "127.0",
            maleToiletCount = "1",
            femaleToiletCount = "1",
            disabledMaleCount = "0",
            disabledFemaleCount = "0",
            childcareCount = "0",
            nursingRoomCount = "0",
        )
        assertThat(item.toExternalData()!!.address).isEqualTo("서울시 강남구 역삼동 123")
    }

    @Test
    fun `toExternalData - 장애인용 여변기수 있으면 disabled=true`() {
        val item = ToiletApiItem(
            name = "화장실",
            roadAddress = "주소",
            lotAddress = null,
            lat = "37.5",
            lng = "127.0",
            maleToiletCount = "0",
            femaleToiletCount = "0",
            disabledMaleCount = "0",
            disabledFemaleCount = "2",
            childcareCount = "0",
            nursingRoomCount = "0",
        )
        assertThat(item.toExternalData()!!.disabled).isTrue()
    }

    /**
     * 테스트용 클라이언트: RestClient를 외부에서 주입받는다.
     */
    class TestablePublicToiletApiClient(
        properties: OpendataProperties,
        private val injectedRestClient: RestClient,
    ) : PublicToiletApiClient(properties) {
        override fun buildRestClient(): RestClient = injectedRestClient
    }
}
