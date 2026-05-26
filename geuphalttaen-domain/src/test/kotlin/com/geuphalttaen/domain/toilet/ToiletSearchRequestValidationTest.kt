package com.geuphalttaen.domain.toilet

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ToiletSearchRequestValidationTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `유효한 좌표는 검증을 통과한다`() {
        val req = ToiletSearchRequest(lat = 37.5665, lng = 126.978)
        assertThat(validator.validate(req)).isEmpty()
    }

    @Test
    fun `경도값을 lat에 전달하면 검증 실패 - Latitude 128 out of range 방지`() {
        val req = ToiletSearchRequest(lat = 128.033, lng = 37.5665)
        val violations = validator.validate(req)
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("lat")
    }

    @Test
    fun `lat가 90 초과면 검증 실패`() {
        val req = ToiletSearchRequest(lat = 91.0, lng = 126.978)
        assertThat(validator.validate(req).map { it.propertyPath.toString() }).contains("lat")
    }

    @Test
    fun `lat가 -90 미만이면 검증 실패`() {
        val req = ToiletSearchRequest(lat = -91.0, lng = 126.978)
        assertThat(validator.validate(req).map { it.propertyPath.toString() }).contains("lat")
    }

    @Test
    fun `lng가 180 초과면 검증 실패`() {
        val req = ToiletSearchRequest(lat = 37.5665, lng = 181.0)
        assertThat(validator.validate(req).map { it.propertyPath.toString() }).contains("lng")
    }
}
