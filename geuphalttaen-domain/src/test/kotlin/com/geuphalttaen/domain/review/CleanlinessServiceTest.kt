package com.geuphalttaen.domain.review

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.CleanlinessEntity
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.toilet.ToiletRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

private fun <T> anyNonNull(type: Class<T>): T = org.mockito.ArgumentMatchers.any(type)

@ExtendWith(MockitoExtension::class)
class CleanlinessServiceTest {

    @Mock
    private lateinit var cleanlinessRepository: CleanlinessRepository

    @Mock
    private lateinit var toiletRepository: ToiletRepository

    private lateinit var cleanlinessService: CleanlinessService

    @BeforeEach
    fun setUp() {
        cleanlinessService = CleanlinessService(cleanlinessRepository, toiletRepository)
    }

    // ──────────────────────────────────────────
    // getMyCleanliness
    // ──────────────────────────────────────────

    @Test
    fun `getMyCleanliness - 내 청결도 평가가 있으면 반환한다`() {
        val toiletId = 1L
        val userId = 10L
        val entity = CleanlinessEntity(id = 1L, toiletId = toiletId, userId = userId, score = 4)

        `when`(toiletRepository.findById(toiletId)).thenReturn(makeToiletEntity(toiletId))
        `when`(cleanlinessRepository.findByToiletIdAndUserId(toiletId, userId)).thenReturn(entity)

        val result = cleanlinessService.getMyCleanliness(userId, toiletId)

        assertThat(result).isNotNull
        assertThat(result!!.score).isEqualTo(4)
        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `getMyCleanliness - 내 청결도 평가가 없으면 null을 반환한다`() {
        val toiletId = 1L
        val userId = 10L

        `when`(toiletRepository.findById(toiletId)).thenReturn(makeToiletEntity(toiletId))
        `when`(cleanlinessRepository.findByToiletIdAndUserId(toiletId, userId)).thenReturn(null)

        val result = cleanlinessService.getMyCleanliness(userId, toiletId)

        assertThat(result).isNull()
    }

    @Test
    fun `getMyCleanliness - 화장실이 없으면 TOILET_NOT_FOUND 예외를 던진다`() {
        `when`(toiletRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy {
            cleanlinessService.getMyCleanliness(userId = 1L, toiletId = 999L)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_NOT_FOUND)
    }

    // ──────────────────────────────────────────
    // upsert
    // ──────────────────────────────────────────

    @Test
    fun `upsert - 기존 평가가 없으면 새로 생성한다`() {
        val toiletId = 1L
        val userId = 10L
        val request = CleanlinessRequest(score = 4)
        val saved = CleanlinessEntity(id = 1L, toiletId = toiletId, userId = userId, score = 4)

        `when`(toiletRepository.findById(toiletId)).thenReturn(makeToiletEntity(toiletId))
        `when`(cleanlinessRepository.findByToiletIdAndUserId(toiletId, userId)).thenReturn(null)
        `when`(cleanlinessRepository.save(anyNonNull(CleanlinessEntity::class.java))).thenReturn(saved)

        val result = cleanlinessService.upsert(userId, toiletId, request)

        assertThat(result.toiletId).isEqualTo(toiletId)
        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.score).isEqualTo(4)
    }

    @Test
    fun `upsert - 기존 평가가 있으면 점수를 업데이트한다`() {
        val toiletId = 1L
        val userId = 10L
        val request = CleanlinessRequest(score = 5)
        val existing = CleanlinessEntity(id = 1L, toiletId = toiletId, userId = userId, score = 3)
        val updated = CleanlinessEntity(id = 1L, toiletId = toiletId, userId = userId, score = 5)

        `when`(toiletRepository.findById(toiletId)).thenReturn(makeToiletEntity(toiletId))
        `when`(cleanlinessRepository.findByToiletIdAndUserId(toiletId, userId)).thenReturn(existing)
        `when`(cleanlinessRepository.save(existing)).thenReturn(updated)

        val result = cleanlinessService.upsert(userId, toiletId, request)

        // 기존 엔티티의 score가 요청값으로 변경되어야 한다
        assertThat(existing.score).isEqualTo(5)
        assertThat(result.score).isEqualTo(5)
    }

    @Test
    fun `upsert - 화장실이 없으면 TOILET_NOT_FOUND 예외를 던진다`() {
        `when`(toiletRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy {
            cleanlinessService.upsert(userId = 1L, toiletId = 999L, request = CleanlinessRequest(score = 3))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_NOT_FOUND)
    }

    // ──────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────

    private fun makeToiletEntity(id: Long = 1L): ToiletEntity = ToiletEntity(
        id = id,
        name = "테스트 화장실",
        address = "서울시 중구 테스트로 1",
        lat = 37.5665,
        lng = 126.9780,
        isPublic = true,
        male = true,
        female = true,
        disabled = false,
        familyRoom = false,
        reportedBy = null,
        status = ToiletStatus.ACTIVE,
    )
}
