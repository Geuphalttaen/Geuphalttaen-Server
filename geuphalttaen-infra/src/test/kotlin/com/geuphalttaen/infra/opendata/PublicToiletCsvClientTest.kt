package com.geuphalttaen.infra.opendata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

class PublicToiletCsvClientTest {

    private val client = PublicToiletCsvClient(OpendataProperties())

    private val charset: Charset = Charsets.UTF_8

    private val headers = "관리ID,화장실명,도로명주소,지번주소,위도,경도,남성용-대변기수,여성용-대변기수,장애인용-남변기수,장애인용-여변기수,영유아보육시설,수유실"

    private fun fetchFromCsv(csv: String) =
        client.fetchFromStream(ByteArrayInputStream(csv.toByteArray(charset)), charset)

    @Test
    fun `정상 행 파싱`() {
        val csv = """
            $headers
            1,강남화장실,서울시 강남구 테헤란로 1,,37.5,127.0,3,2,1,0,0,1
        """.trimIndent()

        val result = fetchFromCsv(csv)

        assertThat(result.items).hasSize(1)
        val item = result.items[0]
        assertThat(item.name).isEqualTo("강남화장실")
        assertThat(item.address).isEqualTo("서울시 강남구 테헤란로 1")
        assertThat(item.lat).isEqualTo(37.5)
        assertThat(item.lng).isEqualTo(127.0)
        assertThat(item.male).isTrue()
        assertThat(item.female).isTrue()
        assertThat(item.disabled).isTrue()
        assertThat(item.familyRoom).isTrue()
    }

    @Test
    fun `위도 파싱 실패 시 parseFailCount 증가`() {
        val csv = """
            $headers
            1,강남화장실,서울시 강남구 테헤란로 1,,N/A,127.0,1,1,0,0,0,0
        """.trimIndent()

        val result = fetchFromCsv(csv)

        assertThat(result.items).isEmpty()
        assertThat(result.parseFailCount).isEqualTo(1)
    }

    @Test
    fun `도로명주소 없으면 지번주소 사용`() {
        val csv = """
            $headers
            1,화장실,,서울시 강남구 역삼동 123,37.5,127.0,1,1,0,0,0,0
        """.trimIndent()

        val result = fetchFromCsv(csv)

        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].address).isEqualTo("서울시 강남구 역삼동 123")
    }

    @Test
    fun `복수 행 정상 처리`() {
        val csv = """
            $headers
            1,화장실A,서울시 강남구 A로 1,,37.1,127.1,1,1,0,0,0,0
            2,화장실B,서울시 강남구 B로 2,,37.2,127.2,1,0,0,0,0,0
        """.trimIndent()

        val result = fetchFromCsv(csv)

        assertThat(result.items).hasSize(2)
        assertThat(result.items.map { it.name }).containsExactly("화장실A", "화장실B")
    }

    @Test
    fun `빈 CSV 처리`() {
        val csv = headers

        val result = fetchFromCsv(csv)

        assertThat(result.items).isEmpty()
        assertThat(result.parseFailCount).isEqualTo(0)
    }

    @Test
    fun `장애인용 여변기수 있으면 disabled=true`() {
        val csv = """
            $headers
            1,화장실,서울시 테스트로 1,,37.5,127.0,0,0,0,2,0,0
        """.trimIndent()

        val result = fetchFromCsv(csv)

        assertThat(result.items[0].disabled).isTrue()
    }
}
