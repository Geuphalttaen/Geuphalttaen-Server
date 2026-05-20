package com.geuphalttaen.domain.admin

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.auth.JwtProvider
import com.geuphalttaen.domain.toilet.AdminToiletResponse
import com.geuphalttaen.domain.toilet.AdminToiletUpdateRequest
import com.geuphalttaen.domain.toilet.ToiletRepository
import com.geuphalttaen.domain.toilet.toAdminResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 관리자 도메인 서비스.
 * 관리자 로그인, 제보 관리, 화장실 CRUD를 처리한다.
 */
@Service
class AdminService(
    private val adminRepository: AdminRepository,
    private val toiletRepository: ToiletRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder,
) {

    /**
     * 관리자 이메일/패스워드 로그인.
     * BCrypt 검증 후 ROLE_ADMIN 클레임이 포함된 JWT를 발급한다.
     */
    fun login(request: AdminLoginRequest): AdminTokenResponse {
        val admin = adminRepository.findByEmail(request.email)
            ?: throw BusinessException(ErrorCode.ADMIN_INVALID_CREDENTIALS)

        if (!passwordEncoder.matches(request.password, admin.passwordHash)) {
            throw BusinessException(ErrorCode.ADMIN_INVALID_CREDENTIALS)
        }

        val accessToken = jwtProvider.generateAdminAccessToken(admin.id)
        return AdminTokenResponse(accessToken = accessToken)
    }

    // ────────────────────────────────────────────
    // 제보 관리
    // ────────────────────────────────────────────

    /**
     * 제보 목록 페이징 조회.
     * status 필터가 없으면 전체를 반환한다.
     */
    fun getReports(status: ToiletStatus?, pageable: Pageable): Page<AdminToiletResponse> {
        val entities = toiletRepository.findByStatusPageable(status, pageable)
        return entities.map { it.toAdminResponse() }
    }

    /**
     * 제보 단건 조회.
     */
    fun getReport(id: Long): AdminToiletResponse {
        val entity = toiletRepository.findById(id) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        return entity.toAdminResponse()
    }

    /**
     * 제보 승인: PENDING → ACTIVE
     */
    @Transactional
    fun approveReport(id: Long): AdminToiletResponse {
        val entity = toiletRepository.findById(id) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        if (entity.status != ToiletStatus.PENDING) {
            throw BusinessException(ErrorCode.TOILET_STATUS_INVALID)
        }
        entity.status = ToiletStatus.ACTIVE
        val saved = toiletRepository.save(entity)
        return saved.toAdminResponse()
    }

    /**
     * 제보 거절: PENDING → REJECTED
     */
    @Transactional
    fun rejectReport(id: Long): AdminToiletResponse {
        val entity = toiletRepository.findById(id) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        if (entity.status != ToiletStatus.PENDING) {
            throw BusinessException(ErrorCode.TOILET_STATUS_INVALID)
        }
        entity.status = ToiletStatus.REJECTED
        val saved = toiletRepository.save(entity)
        return saved.toAdminResponse()
    }

    // ────────────────────────────────────────────
    // 화장실 CRUD
    // ────────────────────────────────────────────

    /**
     * 화장실 목록 검색 (키워드: 이름 또는 주소 포함).
     */
    fun getToilets(keyword: String?, pageable: Pageable): Page<AdminToiletResponse> {
        val entities = toiletRepository.findByKeywordPageable(keyword, pageable)
        return entities.map { it.toAdminResponse() }
    }

    /**
     * 화장실 단건 조회.
     */
    fun getToilet(id: Long): AdminToiletResponse {
        val entity = toiletRepository.findById(id) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        return entity.toAdminResponse()
    }

    /**
     * 화장실 정보 수정.
     */
    @Transactional
    fun updateToilet(id: Long, request: AdminToiletUpdateRequest): AdminToiletResponse {
        val entity = toiletRepository.findById(id) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        request.name?.let { entity.name = it }
        request.address?.let { entity.address = it }
        request.lat?.let { entity.lat = it }
        request.lng?.let { entity.lng = it }
        request.isPublic?.let { entity.isPublic = it }
        request.male?.let { entity.male = it }
        request.female?.let { entity.female = it }
        request.disabled?.let { entity.disabled = it }
        request.familyRoom?.let { entity.familyRoom = it }
        val saved = toiletRepository.save(entity)
        return saved.toAdminResponse()
    }

    /**
     * 화장실 삭제.
     */
    @Transactional
    fun deleteToilet(id: Long) {
        val entity = toiletRepository.findById(id) ?: throw BusinessException(ErrorCode.TOILET_NOT_FOUND)
        toiletRepository.delete(entity)
    }
}
