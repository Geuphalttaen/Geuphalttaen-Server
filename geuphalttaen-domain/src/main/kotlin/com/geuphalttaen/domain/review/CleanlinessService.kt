package com.geuphalttaen.domain.review

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.CleanlinessEntity
import com.geuphalttaen.domain.toilet.ToiletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CleanlinessService(
    private val cleanlinessRepository: CleanlinessRepository,
    private val toiletRepository: ToiletRepository,
) {
    @Transactional(readOnly = true)
    fun getMyCleanliness(userId: Long, toiletId: Long): CleanlinessResponse? {
        toiletRepository.findById(toiletId) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        return cleanlinessRepository.findByToiletIdAndUserId(toiletId, userId)?.toResponse()
    }

    @Transactional
    fun upsert(userId: Long, toiletId: Long, request: CleanlinessRequest): CleanlinessResponse {
        toiletRepository.findById(toiletId) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        val existing = cleanlinessRepository.findByToiletIdAndUserId(toiletId, userId)
        return if (existing != null) {
            // 기존 점수 업데이트
            existing.score = request.score
            cleanlinessRepository.save(existing).toResponse()
        } else {
            val entity = CleanlinessEntity(toiletId = toiletId, userId = userId, score = request.score)
            cleanlinessRepository.save(entity).toResponse()
        }
    }

    private fun CleanlinessEntity.toResponse() = CleanlinessResponse(toiletId, userId, score)
}
