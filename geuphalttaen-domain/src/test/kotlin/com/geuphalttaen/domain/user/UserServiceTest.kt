package com.geuphalttaen.domain.user

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.OAuthProvider
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.core.entity.UserEntity
import com.geuphalttaen.domain.auth.RefreshTokenRepository
import com.geuphalttaen.domain.auth.UserRepository
import com.geuphalttaen.domain.review.CleanlinessRepository
import com.geuphalttaen.domain.review.ReviewRepository
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

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var toiletRepository: ToiletRepository

    @Mock
    private lateinit var reviewRepository: ReviewRepository

    @Mock
    private lateinit var cleanlinessRepository: CleanlinessRepository

    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository, toiletRepository, reviewRepository, cleanlinessRepository, refreshTokenRepository)
    }

    // ──────────────────────────────────────────
    // getProfile
    // ──────────────────────────────────────────

    @Test
    fun `getProfile - 프로필을 정상 반환한다`() {
        val userId = 1L
        `when`(userRepository.findById(userId)).thenReturn(makeUser(userId))
        `when`(toiletRepository.countByReportedBy(userId)).thenReturn(3L)
        `when`(toiletRepository.countByReportedByAndStatus(userId, ToiletStatus.ACTIVE)).thenReturn(2L)

        val result = userService.getProfile(userId)

        assertThat(result.nickname).isEqualTo("테스터")
        assertThat(result.provider).isEqualTo("KAKAO")
        assertThat(result.reportCount).isEqualTo(3)
        assertThat(result.postedCount).isEqualTo(2)
    }

    @Test
    fun `getProfile - 사용자가 없으면 USER_NOT_FOUND 예외를 던진다`() {
        `when`(userRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy { userService.getProfile(999L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.USER_NOT_FOUND)
    }

    // ──────────────────────────────────────────
    // updateNickname
    // ──────────────────────────────────────────

    @Test
    fun `updateNickname - 닉네임을 수정하고 갱신된 프로필을 반환한다`() {
        val userId = 1L
        val user = makeUser(userId, nickname = "기존닉네임")
        `when`(userRepository.findById(userId)).thenReturn(user)
        `when`(userRepository.save(user)).thenReturn(user)
        `when`(toiletRepository.countByReportedBy(userId)).thenReturn(0L)
        `when`(toiletRepository.countByReportedByAndStatus(userId, ToiletStatus.ACTIVE)).thenReturn(0L)

        val result = userService.updateNickname(userId, UpdateNicknameRequest(nickname = "새닉네임"))

        assertThat(result.nickname).isEqualTo("새닉네임")
        verify(userRepository).save(user)
    }

    @Test
    fun `updateNickname - 사용자가 없으면 USER_NOT_FOUND 예외를 던진다`() {
        `when`(userRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy {
            userService.updateNickname(999L, UpdateNicknameRequest(nickname = "닉네임"))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.USER_NOT_FOUND)
    }

    // ──────────────────────────────────────────
    // deleteAccount
    // ──────────────────────────────────────────

    @Test
    fun `deleteAccount - 리뷰, 청결도, 토큰을 삭제하고 사용자를 탈퇴 처리한다`() {
        val userId = 1L
        val user = makeUser(userId)
        `when`(userRepository.findById(userId)).thenReturn(user)

        userService.deleteAccount(userId)

        verify(reviewRepository).deleteAllByUserId(userId)
        verify(cleanlinessRepository).deleteAllByUserId(userId)
        verify(toiletRepository).nullifyReportedBy(userId)
        verify(refreshTokenRepository).delete(userId)
        verify(userRepository).delete(user)
    }

    @Test
    fun `deleteAccount - 사용자가 없으면 USER_NOT_FOUND 예외를 던진다`() {
        `when`(userRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy { userService.deleteAccount(999L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.USER_NOT_FOUND)
    }

    // ──────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────

    private fun makeUser(id: Long = 1L, nickname: String = "테스터"): UserEntity =
        UserEntity(id = id, provider = OAuthProvider.KAKAO, providerId = "kakao_$id", nickname = nickname)
}
