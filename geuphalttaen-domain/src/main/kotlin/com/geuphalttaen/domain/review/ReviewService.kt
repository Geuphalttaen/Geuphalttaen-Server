package com.geuphalttaen.domain.review

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.ReviewEntity
import com.geuphalttaen.domain.auth.UserRepository
import com.geuphalttaen.domain.toilet.ToiletRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val toiletRepository: ToiletRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun addReview(userId: Long, toiletId: Long, request: ReviewRequest): ReviewResponse {
        toiletRepository.findById(toiletId) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        if (reviewRepository.existsByToiletIdAndUserId(toiletId, userId)) {
            throw BusinessException(ErrorCode.REVIEW_DUPLICATE)
        }
        val nickname = userRepository.findById(userId)?.nickname
        val entity = ReviewEntity(toiletId = toiletId, userId = userId, rating = request.rating, content = request.content)
        return reviewRepository.save(entity).toResponse(nickname)
    }

    @Transactional(readOnly = true)
    fun getReviews(toiletId: Long, pageable: Pageable): Page<ReviewResponse> {
        toiletRepository.findById(toiletId) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        val page = reviewRepository.findByToiletIdPageable(toiletId, pageable)
        val nicknameMap = userRepository.findAllByIds(page.content.map { it.userId })
            .associate { it.id to it.nickname }
        return page.map { it.toResponse(nicknameMap[it.userId]) }
    }

    @Transactional(readOnly = true)
    fun getMyReview(userId: Long, toiletId: Long): ReviewResponse? {
        toiletRepository.findById(toiletId) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        val nickname = userRepository.findById(userId)?.nickname
        return reviewRepository.findByToiletIdAndUserId(toiletId, userId)?.toResponse(nickname)
    }

    @Transactional
    fun updateMyReview(userId: Long, toiletId: Long, request: ReviewRequest): ReviewResponse {
        toiletRepository.findById(toiletId) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        val entity = reviewRepository.findByToiletIdAndUserId(toiletId, userId)
            ?: throw BusinessException(ErrorCode.REVIEW_NOT_FOUND)
        val nickname = userRepository.findById(userId)?.nickname
        entity.rating = request.rating
        entity.content = request.content
        return reviewRepository.save(entity).toResponse(nickname)
    }

    // 관리자용 — 화장실 존재 여부 검증 없이 조회
    @Transactional(readOnly = true)
    fun getReviewsForAdmin(toiletId: Long, pageable: Pageable): Page<ReviewResponse> {
        val page = reviewRepository.findByToiletIdPageable(toiletId, pageable)
        val nicknameMap = userRepository.findAllByIds(page.content.map { it.userId })
            .associate { it.id to it.nickname }
        return page.map { it.toResponse(nicknameMap[it.userId]) }
    }

    // 관리자용 — 리뷰 삭제
    @Transactional
    fun deleteReview(id: Long) {
        val entity = reviewRepository.findById(id) ?: throw BusinessException(ErrorCode.REVIEW_NOT_FOUND)
        reviewRepository.delete(entity)
    }
}
