package com.geuphalttaen.domain.review

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.OAuthProvider
import com.geuphalttaen.core.entity.ReviewEntity
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.core.entity.UserEntity
import com.geuphalttaen.domain.auth.UserRepository
import com.geuphalttaen.domain.toilet.ToiletRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

private fun <T> anyNonNull(type: Class<T>): T = org.mockito.ArgumentMatchers.any(type)

@ExtendWith(MockitoExtension::class)
class ReviewServiceTest {

    @Mock
    private lateinit var reviewRepository: ReviewRepository

    @Mock
    private lateinit var toiletRepository: ToiletRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var reviewService: ReviewService

    @BeforeEach
    fun setUp() {
        reviewService = ReviewService(reviewRepository, toiletRepository, userRepository)
    }

    // ──────────────────────────────────────────
    // addReview
    // ──────────────────────────────────────────

    @Test
    fun `addReview - 성공적으로 리뷰를 저장하고 응답을 반환한다`() {
        val toiletId = 1L
        val userId = 10L
        val request = ReviewRequest(rating = 4, content = "깨끗해요")
        val entity = makeReviewEntity(id = 1L, toiletId = toiletId, userId = userId, rating = 4, content = "깨끗해요")

        `when`(toiletRepository.findById(toiletId)).thenReturn(makeToiletEntity(toiletId))
        `when`(reviewRepository.existsByToiletIdAndUserId(toiletId, userId)).thenReturn(false)
        `when`(reviewRepository.save(anyNonNull(ReviewEntity::class.java))).thenReturn(entity)
        `when`(userRepository.findById(userId)).thenReturn(makeUserEntity(userId, "테스터"))

        val result = reviewService.addReview(userId, toiletId, request)

        assertThat(result.toiletId).isEqualTo(toiletId)
        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.rating).isEqualTo(4)
        assertThat(result.content).isEqualTo("깨끗해요")
        assertThat(result.nickname).isEqualTo("테스터")
    }

    @Test
    fun `addReview - 화장실이 없으면 TOILET_NOT_FOUND 예외를 던진다`() {
        `when`(toiletRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy {
            reviewService.addReview(userId = 1L, toiletId = 999L, request = ReviewRequest(rating = 3))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_NOT_FOUND)
    }

    @Test
    fun `addReview - 이미 리뷰가 존재하면 REVIEW_DUPLICATE 예외를 던진다`() {
        val toiletId = 1L
        val userId = 10L
        `when`(toiletRepository.findById(toiletId)).thenReturn(makeToiletEntity(toiletId))
        `when`(reviewRepository.existsByToiletIdAndUserId(toiletId, userId)).thenReturn(true)

        assertThatThrownBy {
            reviewService.addReview(userId, toiletId, ReviewRequest(rating = 5))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.REVIEW_DUPLICATE)
    }

    // ──────────────────────────────────────────
    // getReviews
    // ──────────────────────────────────────────

    @Test
    fun `getReviews - 화장실 리뷰 목록을 페이징 조회한다`() {
        val toiletId = 1L
        val userId = 10L
        val pageable = PageRequest.of(0, 10)
        val entities = listOf(makeReviewEntity(id = 1L, toiletId = toiletId, userId = userId, rating = 4))
        `when`(toiletRepository.findById(toiletId)).thenReturn(makeToiletEntity(toiletId))
        `when`(reviewRepository.findByToiletIdPageable(toiletId, pageable))
            .thenReturn(PageImpl(entities, pageable, 1L))
        `when`(userRepository.findAllByIds(listOf(userId))).thenReturn(listOf(makeUserEntity(userId, "테스터")))

        val result = reviewService.getReviews(toiletId, pageable)

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content[0].rating).isEqualTo(4)
        assertThat(result.content[0].nickname).isEqualTo("테스터")
    }

    @Test
    fun `getReviews - 화장실이 없으면 TOILET_NOT_FOUND 예외를 던진다`() {
        `when`(toiletRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy {
            reviewService.getReviews(999L, PageRequest.of(0, 10))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_NOT_FOUND)
    }

    // ──────────────────────────────────────────
    // deleteReview
    // ──────────────────────────────────────────

    @Test
    fun `deleteReview - 존재하는 리뷰를 삭제한다`() {
        val entity = makeReviewEntity(id = 5L, toiletId = 1L, userId = 10L, rating = 3)
        `when`(reviewRepository.findById(5L)).thenReturn(entity)

        reviewService.deleteReview(5L)

        verify(reviewRepository).delete(entity)
    }

    @Test
    fun `deleteReview - 존재하지 않는 리뷰는 REVIEW_NOT_FOUND 예외를 던진다`() {
        `when`(reviewRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy {
            reviewService.deleteReview(999L)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.REVIEW_NOT_FOUND)
    }

    // ──────────────────────────────────────────
    // getMyReview
    // ──────────────────────────────────────────

    @Test
    fun `getMyReview - 내 리뷰가 있으면 반환한다`() {
        val toiletId = 1L
        val userId = 10L
        val entity = makeReviewEntity(id = 3L, toiletId = toiletId, userId = userId, rating = 5)
        `when`(toiletRepository.findById(toiletId)).thenReturn(makeToiletEntity(toiletId))
        `when`(reviewRepository.findByToiletIdAndUserId(toiletId, userId)).thenReturn(entity)

        val result = reviewService.getMyReview(userId, toiletId)

        assertThat(result).isNotNull
        assertThat(result!!.rating).isEqualTo(5)
        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `getMyReview - 내 리뷰가 없으면 null을 반환한다`() {
        val toiletId = 1L
        val userId = 10L
        `when`(toiletRepository.findById(toiletId)).thenReturn(makeToiletEntity(toiletId))
        `when`(reviewRepository.findByToiletIdAndUserId(toiletId, userId)).thenReturn(null)

        val result = reviewService.getMyReview(userId, toiletId)

        assertThat(result).isNull()
    }

    @Test
    fun `getMyReview - 화장실이 없으면 TOILET_NOT_FOUND 예외를 던진다`() {
        `when`(toiletRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy {
            reviewService.getMyReview(userId = 1L, toiletId = 999L)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_NOT_FOUND)
    }

    // ──────────────────────────────────────────
    // updateMyReview
    // ──────────────────────────────────────────

    @Test
    fun `updateMyReview - 기존 리뷰를 수정하고 반환한다`() {
        val toiletId = 1L
        val userId = 10L
        val entity = makeReviewEntity(id = 2L, toiletId = toiletId, userId = userId, rating = 3, content = "보통")
        `when`(toiletRepository.findById(toiletId)).thenReturn(makeToiletEntity(toiletId))
        `when`(reviewRepository.findByToiletIdAndUserId(toiletId, userId)).thenReturn(entity)
        `when`(reviewRepository.save(entity)).thenReturn(entity)

        val result = reviewService.updateMyReview(userId, toiletId, ReviewRequest(rating = 5, content = "좋아요"))

        assertThat(result.rating).isEqualTo(5)
        assertThat(result.content).isEqualTo("좋아요")
        verify(reviewRepository).save(entity)
    }

    @Test
    fun `updateMyReview - 리뷰가 없으면 REVIEW_NOT_FOUND 예외를 던진다`() {
        val toiletId = 1L
        val userId = 10L
        `when`(toiletRepository.findById(toiletId)).thenReturn(makeToiletEntity(toiletId))
        `when`(reviewRepository.findByToiletIdAndUserId(toiletId, userId)).thenReturn(null)

        assertThatThrownBy {
            reviewService.updateMyReview(userId, toiletId, ReviewRequest(rating = 4))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.REVIEW_NOT_FOUND)
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

    private fun makeReviewEntity(
        id: Long = 1L,
        toiletId: Long = 1L,
        userId: Long = 10L,
        rating: Int = 4,
        content: String? = null,
    ): ReviewEntity = ReviewEntity(
        id = id,
        toiletId = toiletId,
        userId = userId,
        rating = rating,
        content = content,
    )

    private fun makeUserEntity(id: Long = 1L, nickname: String = "사용자"): UserEntity =
        UserEntity(id = id, provider = OAuthProvider.KAKAO, providerId = "kakao_$id", nickname = nickname)
}
