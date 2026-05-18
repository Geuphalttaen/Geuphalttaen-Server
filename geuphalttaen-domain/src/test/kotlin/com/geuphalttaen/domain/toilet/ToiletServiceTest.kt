package com.geuphalttaen.domain.toilet

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Kotlin에서 Mockito any()가 null을 반환하는 문제를 우회하는 헬퍼.
 */
private fun <T> anyNonNull(type: Class<T>): T = org.mockito.ArgumentMatchers.any(type)

@ExtendWith(MockitoExtension::class)
class ToiletServiceTest {

    @Mock
    private lateinit var toiletRepository: ToiletRepository

    private lateinit var toiletService: ToiletService

    @BeforeEach
    fun setUp() {
        toiletService = ToiletService(toiletRepository)
    }

    // ──────────────────────────────────────────
    // searchNearby
    // ──────────────────────────────────────────

    @Test
    fun `searchNearby - 반경 내 화장실 목록을 반환한다`() {
        val lat = 37.5665
        val lng = 126.9780
        val entity = makeToiletEntity(id = 1L, lat = lat, lng = lng)
        `when`(toiletRepository.findNearby(lat, lng, 1000)).thenReturn(listOf(entity))

        val result = toiletService.searchNearby(ToiletSearchRequest(lat = lat, lng = lng))

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(1L)
        assertThat(result[0].name).isEqualTo("테스트 화장실")
        assertThat(result[0].familyRoom).isFalse()
    }

    @Test
    fun `searchNearby - 반경 내 화장실이 없으면 빈 목록을 반환한다`() {
        val lat = 37.5665
        val lng = 126.9780
        `when`(toiletRepository.findNearby(lat, lng, 500)).thenReturn(emptyList())

        val result = toiletService.searchNearby(ToiletSearchRequest(lat = lat, lng = lng, radiusMeters = 500))

        assertThat(result).isEmpty()
    }

    // ──────────────────────────────────────────
    // getById
    // ──────────────────────────────────────────

    @Test
    fun `getById - 존재하는 id로 조회하면 ToiletResponse를 반환한다`() {
        val entity = makeToiletEntity(id = 42L, familyRoom = true)
        `when`(toiletRepository.findById(42L)).thenReturn(entity)

        val result = toiletService.getById(42L)

        assertThat(result.id).isEqualTo(42L)
        assertThat(result.name).isEqualTo("테스트 화장실")
        assertThat(result.familyRoom).isTrue()
        assertThat(result.distanceMeters).isNull()
    }

    @Test
    fun `getById - 존재하지 않는 id는 TOILET_NOT_FOUND 예외를 던진다`() {
        `when`(toiletRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy { toiletService.getById(999L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_NOT_FOUND)
    }

    // ──────────────────────────────────────────
    // report
    // ──────────────────────────────────────────

    @Test
    fun `report - 요청 정보로 ToiletEntity를 저장하고 ToiletResponse를 반환한다`() {
        val request = ToiletReportRequest(
            name = "새 화장실",
            address = "서울시 중구 1번지",
            lat = 37.5665,
            lng = 126.9780,
            isPublic = false,
            male = true,
            female = true,
            disabled = true,
            familyRoom = true,
        )
        val savedEntity = makeToiletEntity(
            id = 10L,
            lat = request.lat,
            lng = request.lng,
            familyRoom = true,
            status = ToiletStatus.PENDING,
        ).apply {
            name = request.name
            address = request.address
            disabled = true
            isPublic = false
            reportedBy = 7L
        }

        `when`(toiletRepository.save(anyNonNull(ToiletEntity::class.java))).thenReturn(savedEntity)

        val result = toiletService.report(userId = 7L, request = request)

        assertThat(result.id).isEqualTo(10L)
        assertThat(result.familyRoom).isTrue()
        assertThat(result.disabled).isTrue()
    }

    @Test
    fun `report - familyRoom 기본값은 false이다`() {
        val request = ToiletReportRequest(
            name = "기본 화장실",
            address = "서울시 강남구 1번지",
            lat = 37.5050,
            lng = 127.0490,
        )
        val savedEntity = makeToiletEntity(
            id = 20L,
            lat = request.lat,
            lng = request.lng,
            familyRoom = false,
        )

        `when`(toiletRepository.save(anyNonNull(ToiletEntity::class.java))).thenReturn(savedEntity)

        val result = toiletService.report(userId = 1L, request = request)

        assertThat(result.familyRoom).isFalse()
    }

    // ──────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────

    private fun makeToiletEntity(
        id: Long = 1L,
        lat: Double = 37.5665,
        lng: Double = 126.9780,
        familyRoom: Boolean = false,
        status: ToiletStatus = ToiletStatus.ACTIVE,
    ): ToiletEntity = ToiletEntity(
        id = id,
        name = "테스트 화장실",
        address = "서울시 중구 테스트로 1",
        lat = lat,
        lng = lng,
        isPublic = true,
        male = true,
        female = true,
        disabled = false,
        familyRoom = familyRoom,
        reportedBy = null,
        status = status,
    )
}
