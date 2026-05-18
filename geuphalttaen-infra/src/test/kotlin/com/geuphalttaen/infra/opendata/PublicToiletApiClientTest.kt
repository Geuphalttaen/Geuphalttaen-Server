package com.geuphalttaen.infra.opendata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PublicToiletApiClientTest {

    private fun makeItem(
        name: String? = "테스트화장실",
        roadAddress: String? = "서울시 강남구 테헤란로 1",
        lotAddress: String? = null,
        lat: String? = "37.5",
        lng: String? = "127.0",
        maleToiletCount: String? = "2",
        femaleToiletCount: String? = "2",
        disabledMaleCount: String? = "0",
        disabledFemaleCount: String? = "0",
        familyMaleCount: String? = "0",
        familyFemaleCount: String? = "0",
    ) = ToiletApiItem(
        name = name, roadAddress = roadAddress, lotAddress = lotAddress,
        lat = lat, lng = lng, maleToiletCount = maleToiletCount, femaleToiletCount = femaleToiletCount,
        disabledMaleCount = disabledMaleCount, disabledFemaleCount = disabledFemaleCount,
        familyMaleCount = familyMaleCount, familyFemaleCount = familyFemaleCount,
    )

    @Test fun `정상 데이터 매핑`() {
        val dto = makeItem().toDto()
        assertThat(dto.name).isEqualTo("테스트화장실")
        assertThat(dto.address).isEqualTo("서울시 강남구 테헤란로 1")
        assertThat(dto.lat).isEqualTo(37.5)
        assertThat(dto.lng).isEqualTo(127.0)
        assertThat(dto.male).isTrue()
        assertThat(dto.female).isTrue()
        assertThat(dto.disabled).isFalse()
        assertThat(dto.familyRoom).isFalse()
    }

    @Test fun `도로명주소가 없으면 지번주소 사용`() {
        assertThat(makeItem(roadAddress = null, lotAddress = "서울시 강남구 역삼동 123").toDto().address).isEqualTo("서울시 강남구 역삼동 123")
    }

    @Test fun `도로명주소가 빈 문자열이면 지번주소 사용`() {
        assertThat(makeItem(roadAddress = "", lotAddress = "서울시 강남구 역삼동 123").toDto().address).isEqualTo("서울시 강남구 역삼동 123")
    }

    @Test fun `도로명 지번 주소 모두 없으면 빈 문자열`() {
        assertThat(makeItem(roadAddress = null, lotAddress = null).toDto().address).isEqualTo("")
    }

    @Test fun `lat이 null이면 0으로 매핑`() {
        assertThat(makeItem(lat = null).toDto().lat).isEqualTo(0.0)
    }

    @Test fun `lng이 null이면 0으로 매핑`() {
        assertThat(makeItem(lng = null).toDto().lng).isEqualTo(0.0)
    }

    @Test fun `lat이 파싱 불가면 0으로 매핑`() {
        assertThat(makeItem(lat = "N/A").toDto().lat).isEqualTo(0.0)
    }

    @Test fun `lng이 파싱 불가면 0으로 매핑`() {
        assertThat(makeItem(lng = "").toDto().lng).isEqualTo(0.0)
    }

    @Test fun `남성용 대변기 수가 0이면 male=false`() {
        val dto = makeItem(maleToiletCount = "0", femaleToiletCount = "2").toDto()
        assertThat(dto.male).isFalse()
        assertThat(dto.female).isTrue()
    }

    @Test fun `장애인용 대변기 수가 있으면 disabled=true`() {
        assertThat(makeItem(disabledMaleCount = "1").toDto().disabled).isTrue()
    }

    @Test fun `장애인용 여성 대변기 수가 있으면 disabled=true`() {
        assertThat(makeItem(disabledFemaleCount = "1").toDto().disabled).isTrue()
    }

    @Test fun `어린이용 남성 대변기 수가 있으면 familyRoom=true`() {
        assertThat(makeItem(familyMaleCount = "1").toDto().familyRoom).isTrue()
    }

    @Test fun `어린이용 여성 대변기 수가 있으면 familyRoom=true`() {
        assertThat(makeItem(familyFemaleCount = "2").toDto().familyRoom).isTrue()
    }

    @Test fun `name이 null이면 빈 문자열로 매핑`() {
        assertThat(makeItem(name = null).toDto().name).isEqualTo("")
    }
}
