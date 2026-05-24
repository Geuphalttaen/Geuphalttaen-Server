package com.geuphalttaen.domain.toilet

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletImageEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.image.ImageService
import com.geuphalttaen.domain.review.CleanlinessRepository
import com.geuphalttaen.domain.review.ReviewRepository
import com.geuphalttaen.domain.review.ReviewStats
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing

private fun <T> anyNonNull(type: Class<T>): T = org.mockito.ArgumentMatchers.any(type)

@ExtendWith(MockitoExtension::class)
class ToiletServiceTest {

    @Mock
    private lateinit var toiletRepository: ToiletRepository

    @Mock
    private lateinit var imageService: ImageService

    @Mock
    private lateinit var reviewRepository: ReviewRepository

    @Mock
    private lateinit var cleanlinessRepository: CleanlinessRepository

    private lateinit var toiletService: ToiletService

    @BeforeEach
    fun setUp() {
        toiletService = ToiletService(toiletRepository, imageService, reviewRepository, cleanlinessRepository)
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
        `when`(toiletRepository.findImagesByToiletIds(listOf(1L))).thenReturn(emptyList())
        `when`(reviewRepository.findStatsByToiletIds(listOf(1L))).thenReturn(emptyMap())
        `when`(cleanlinessRepository.findAveragesByToiletIds(listOf(1L))).thenReturn(emptyMap())

        val result = toiletService.searchNearby(ToiletSearchRequest(lat = lat, lng = lng))

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(1L)
        assertThat(result[0].imageUrls).isEmpty()
    }

    @Test
    fun `searchNearby - 반경 내 화장실이 없으면 빈 목록을 반환한다`() {
        val lat = 37.5665
        val lng = 126.9780
        `when`(toiletRepository.findNearby(lat, lng, 500)).thenReturn(emptyList())
        `when`(toiletRepository.findImagesByToiletIds(emptyList())).thenReturn(emptyList())

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
        `when`(toiletRepository.findImagesByToiletId(42L)).thenReturn(emptyList())
        `when`(reviewRepository.findStatsByToiletId(42L)).thenReturn(ReviewStats(null, 0L))
        `when`(cleanlinessRepository.findAverageByToiletId(42L)).thenReturn(null)

        val result = toiletService.getById(42L)

        assertThat(result.id).isEqualTo(42L)
        assertThat(result.familyRoom).isTrue()
        assertThat(result.imageUrls).isEmpty()
    }

    @Test
    fun `getById - 이미지가 있는 화장실은 imageUrls를 포함해 반환한다`() {
        val entity = makeToiletEntity(id = 42L)
        val images = listOf(
            ToiletImageEntity(toiletId = 42L, url = "https://cdn.example.com/img1.webp", originalUrl = "https://cdn.example.com/orig1.jpg"),
            ToiletImageEntity(toiletId = 42L, url = "https://cdn.example.com/img2.webp", originalUrl = "https://cdn.example.com/orig2.jpg"),
        )
        `when`(toiletRepository.findById(42L)).thenReturn(entity)
        `when`(toiletRepository.findImagesByToiletId(42L)).thenReturn(images)
        `when`(reviewRepository.findStatsByToiletId(42L)).thenReturn(ReviewStats(null, 0L))
        `when`(cleanlinessRepository.findAverageByToiletId(42L)).thenReturn(null)

        val result = toiletService.getById(42L)

        assertThat(result.imageUrls).containsExactly(
            "https://cdn.example.com/img1.webp",
            "https://cdn.example.com/img2.webp",
        )
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
    fun `report - images 없이 제보하면 이미지 없이 저장된다`() {
        val request = ToiletReportRequest(
            name = "새 화장실",
            address = "서울시 중구 1번지",
            lat = 37.5665,
            lng = 126.9780,
        )
        val savedEntity = makeToiletEntity(id = 10L, lat = request.lat, lng = request.lng)
        `when`(toiletRepository.save(anyNonNull(ToiletEntity::class.java))).thenReturn(savedEntity)

        val result = toiletService.report(userId = 7L, request = request)

        assertThat(result.id).isEqualTo(10L)
        assertThat(result.imageUrls).isEmpty()
    }

    @Test
    fun `report - images를 포함하면 이미지 엔티티를 저장한다`() {
        val imageRefs = listOf(
            ImageRef(url = "https://cdn.example.com/webp1.webp", originalUrl = "https://cdn.example.com/orig1.jpg"),
            ImageRef(url = "https://cdn.example.com/webp2.webp", originalUrl = "https://cdn.example.com/orig2.jpg"),
        )
        val request = ToiletReportRequest(
            name = "새 화장실",
            address = "서울시 중구 1번지",
            lat = 37.5665,
            lng = 126.9780,
            images = imageRefs,
        )
        val savedEntity = makeToiletEntity(id = 10L, lat = request.lat, lng = request.lng)
        val savedImages = imageRefs.map {
            ToiletImageEntity(toiletId = 10L, url = it.url, originalUrl = it.originalUrl)
        }

        doNothing().`when`(imageService).validateUrls(any())
        `when`(toiletRepository.save(anyNonNull(ToiletEntity::class.java))).thenReturn(savedEntity)
        `when`(toiletRepository.saveImages(any())).thenReturn(savedImages)

        val result = toiletService.report(userId = 7L, request = request)

        assertThat(result.id).isEqualTo(10L)
        assertThat(result.imageUrls).containsExactly(
            "https://cdn.example.com/webp1.webp",
            "https://cdn.example.com/webp2.webp",
        )
    }

    @Test
    fun `report - familyRoom 기본값은 false이다`() {
        val request = ToiletReportRequest(
            name = "기본 화장실",
            address = "서울시 강남구 1번지",
            lat = 37.5050,
            lng = 127.0490,
        )
        val savedEntity = makeToiletEntity(id = 20L, lat = request.lat, lng = request.lng, familyRoom = false)
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
