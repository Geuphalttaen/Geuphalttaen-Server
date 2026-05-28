package com.geuphalttaen.domain.user

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.core.entity.UserEntity
import com.geuphalttaen.domain.auth.RefreshTokenRepository
import com.geuphalttaen.domain.auth.UserRepository
import com.geuphalttaen.domain.review.CleanlinessRepository
import com.geuphalttaen.domain.review.ReviewRepository
import com.geuphalttaen.domain.toilet.ToiletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
class UserService(
    private val userRepository: UserRepository,
    private val toiletRepository: ToiletRepository,
    private val reviewRepository: ReviewRepository,
    private val cleanlinessRepository: CleanlinessRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    private val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    fun getProfile(userId: Long): UserProfileResponse {
        val user = userRepository.findById(userId) ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        return buildProfileResponse(user)
    }

    @Transactional
    fun updateNickname(userId: Long, request: UpdateNicknameRequest): UserProfileResponse {
        val user = userRepository.findById(userId) ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        user.nickname = request.nickname
        userRepository.save(user)
        return buildProfileResponse(user)
    }

    private fun buildProfileResponse(user: UserEntity): UserProfileResponse {
        val reportCount = toiletRepository.countByReportedBy(user.id)
        val postedCount = toiletRepository.countByReportedByAndStatus(user.id, ToiletStatus.ACTIVE)
        return UserProfileResponse(
            nickname = user.nickname,
            provider = user.provider.name,
            reportCount = reportCount.toInt(),
            postedCount = postedCount.toInt(),
        )
    }

    @Transactional
    fun deleteAccount(userId: Long) {
        val user = userRepository.findById(userId) ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        reviewRepository.deleteAllByUserId(userId)
        cleanlinessRepository.deleteAllByUserId(userId)
        toiletRepository.nullifyReportedBy(userId)
        refreshTokenRepository.delete(userId)
        userRepository.delete(user)
    }

    fun getMyReports(userId: Long): List<MyReportResponse> {
        userRepository.findById(userId) ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        return toiletRepository.findByReportedByOrderByCreatedAtDesc(userId).map { toilet ->
            MyReportResponse(
                id = toilet.id,
                name = toilet.name,
                address = toilet.address,
                lat = toilet.lat,
                lng = toilet.lng,
                status = toilet.status,
                createdAt = toilet.createdAt.format(formatter),
            )
        }
    }
}
