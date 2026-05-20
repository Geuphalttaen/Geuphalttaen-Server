package com.geuphalttaen.domain.admin

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import com.geuphalttaen.core.entity.AdminEntity
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.auth.JwtProperties
import com.geuphalttaen.domain.auth.JwtProvider
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * Kotlin에서 Mockito any()가 null을 반환하는 문제를 우회하는 헬퍼.
 */
private fun <T> anyNonNull(type: Class<T>): T = org.mockito.ArgumentMatchers.any(type)

@ExtendWith(MockitoExtension::class)
class AdminServiceTest {

    @Mock
    private lateinit var adminRepository: AdminRepository

    @Mock
    private lateinit var toiletRepository: ToiletRepository

    private lateinit var jwtProvider: JwtProvider
    private lateinit var adminService: AdminService
    private lateinit var passwordEncoder: BCryptPasswordEncoder

    private val testSecret = "test-secret-key-must-be-at-least-32-characters-long-for-hmac"
    private val testSeedSecret = "test-seed-secret"

    @BeforeEach
    fun setUp() {
        jwtProvider = JwtProvider(JwtProperties(secret = testSecret))
        passwordEncoder = BCryptPasswordEncoder()
        adminService = AdminService(
            adminRepository,
            toiletRepository,
            jwtProvider,
            passwordEncoder,
            AdminProperties(seedSecret = testSeedSecret),
        )
    }

    // ────────────────────────────────────────────
    // 로그인
    // ────────────────────────────────────────────

    @Test
    fun `login - 올바른 이메일과 비밀번호로 AccessToken을 반환한다`() {
        val rawPassword = "password123"
        val hashed = passwordEncoder.encode(rawPassword)
        val admin = AdminEntity(id = 1L, email = "admin@test.com", passwordHash = hashed)
        `when`(adminRepository.findByEmail("admin@test.com")).thenReturn(admin)

        val result = adminService.login(AdminLoginRequest(email = "admin@test.com", password = rawPassword))

        assertThat(result.accessToken).isNotBlank()
        // 발급된 토큰에 ROLE_ADMIN이 포함되어야 한다
        assertThat(jwtProvider.getRoles(result.accessToken)).contains("ROLE_ADMIN")
    }

    @Test
    fun `login - 존재하지 않는 이메일은 ADMIN_INVALID_CREDENTIALS 예외를 던진다`() {
        `when`(adminRepository.findByEmail("notfound@test.com")).thenReturn(null)

        assertThatThrownBy {
            adminService.login(AdminLoginRequest(email = "notfound@test.com", password = "any"))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.ADMIN_INVALID_CREDENTIALS)
    }

    @Test
    fun `login - 비밀번호가 틀리면 ADMIN_INVALID_CREDENTIALS 예외를 던진다`() {
        val hashed = passwordEncoder.encode("correct-password")
        val admin = AdminEntity(id = 1L, email = "admin@test.com", passwordHash = hashed)
        `when`(adminRepository.findByEmail("admin@test.com")).thenReturn(admin)

        assertThatThrownBy {
            adminService.login(AdminLoginRequest(email = "admin@test.com", password = "wrong-password"))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.ADMIN_INVALID_CREDENTIALS)
    }

    // ────────────────────────────────────────────
    // 제보 통계
    // ────────────────────────────────────────────

    @Test
    fun `getReportStats - 각 상태별 카운트를 올바르게 반환한다`() {
        `when`(toiletRepository.countByStatus(ToiletStatus.PENDING)).thenReturn(3L)
        `when`(toiletRepository.countByStatus(ToiletStatus.ACTIVE)).thenReturn(10L)
        `when`(toiletRepository.countByStatus(ToiletStatus.REJECTED)).thenReturn(2L)

        val result = adminService.getReportStats()

        assertThat(result.pending).isEqualTo(3)
        assertThat(result.active).isEqualTo(10)
        assertThat(result.rejected).isEqualTo(2)
    }

    // ────────────────────────────────────────────
    // 제보 관리
    // ────────────────────────────────────────────

    @Test
    fun `getReports - status 필터가 없으면 전체 페이지를 반환한다`() {
        val pageable = PageRequest.of(0, 10)
        val entities = listOf(makeToiletEntity(id = 1L, status = ToiletStatus.PENDING))
        `when`(toiletRepository.findByStatusPageable(null, pageable))
            .thenReturn(PageImpl(entities, pageable, 1L))

        val result = adminService.getReports(null, pageable)

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content[0].id).isEqualTo(1L)
    }

    @Test
    fun `getReports - PENDING 필터로 조회하면 PENDING 항목만 반환한다`() {
        val pageable = PageRequest.of(0, 10)
        val entities = listOf(makeToiletEntity(id = 2L, status = ToiletStatus.PENDING))
        `when`(toiletRepository.findByStatusPageable(ToiletStatus.PENDING, pageable))
            .thenReturn(PageImpl(entities, pageable, 1L))

        val result = adminService.getReports(ToiletStatus.PENDING, pageable)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].status).isEqualTo(ToiletStatus.PENDING)
    }

    @Test
    fun `getReport - 존재하는 id로 단건 조회에 성공한다`() {
        val entity = makeToiletEntity(id = 5L)
        `when`(toiletRepository.findById(5L)).thenReturn(entity)

        val result = adminService.getReport(5L)

        assertThat(result.id).isEqualTo(5L)
    }

    @Test
    fun `getReport - 존재하지 않는 id는 TOILET_NOT_FOUND 예외를 던진다`() {
        `when`(toiletRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy { adminService.getReport(999L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_NOT_FOUND)
    }

    @Test
    fun `approveReport - PENDING 제보를 ACTIVE로 변경한다`() {
        val entity = makeToiletEntity(id = 10L, status = ToiletStatus.PENDING)
        `when`(toiletRepository.findById(10L)).thenReturn(entity)

        val result = adminService.approveReport(10L)

        assertThat(result.status).isEqualTo(ToiletStatus.ACTIVE)
    }

    @Test
    fun `approveReport - PENDING이 아닌 제보를 승인하면 TOILET_STATUS_INVALID 예외를 던진다`() {
        val entity = makeToiletEntity(id = 11L, status = ToiletStatus.ACTIVE)
        `when`(toiletRepository.findById(11L)).thenReturn(entity)

        assertThatThrownBy { adminService.approveReport(11L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_STATUS_INVALID)
    }

    @Test
    fun `approveReport - 존재하지 않는 id는 TOILET_NOT_FOUND 예외를 던진다`() {
        `when`(toiletRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy { adminService.approveReport(999L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_NOT_FOUND)
    }

    @Test
    fun `rejectReport - PENDING 제보를 REJECTED로 변경한다`() {
        val entity = makeToiletEntity(id = 20L, status = ToiletStatus.PENDING)
        `when`(toiletRepository.findById(20L)).thenReturn(entity)

        val result = adminService.rejectReport(20L)

        assertThat(result.status).isEqualTo(ToiletStatus.REJECTED)
    }

    @Test
    fun `rejectReport - PENDING이 아닌 제보를 거절하면 TOILET_STATUS_INVALID 예외를 던진다`() {
        val entity = makeToiletEntity(id = 21L, status = ToiletStatus.REJECTED)
        `when`(toiletRepository.findById(21L)).thenReturn(entity)

        assertThatThrownBy { adminService.rejectReport(21L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_STATUS_INVALID)
    }

    @Test
    fun `rejectReport - 존재하지 않는 id는 TOILET_NOT_FOUND 예외를 던진다`() {
        `when`(toiletRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy { adminService.rejectReport(999L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_NOT_FOUND)
    }

    // ────────────────────────────────────────────
    // 화장실 CRUD
    // ────────────────────────────────────────────

    @Test
    fun `getToilets - 키워드 없이 전체 목록을 페이징 조회한다`() {
        val pageable = PageRequest.of(0, 20)
        val entities = listOf(makeToiletEntity(id = 1L))
        `when`(toiletRepository.findByKeywordPageable(null, pageable))
            .thenReturn(PageImpl(entities, pageable, 1L))

        val result = adminService.getToilets(null, pageable)

        assertThat(result.content).hasSize(1)
    }

    @Test
    fun `getToilets - 키워드로 검색하면 해당 화장실을 반환한다`() {
        val pageable = PageRequest.of(0, 20)
        val entity = makeToiletEntity(id = 1L).apply { name = "강남 화장실" }
        `when`(toiletRepository.findByKeywordPageable("강남", pageable))
            .thenReturn(PageImpl(listOf(entity), pageable, 1L))

        val result = adminService.getToilets("강남", pageable)

        assertThat(result.content[0].name).isEqualTo("강남 화장실")
    }

    @Test
    fun `getToilet - 존재하는 id로 단건 조회에 성공한다`() {
        val entity = makeToiletEntity(id = 30L)
        `when`(toiletRepository.findById(30L)).thenReturn(entity)

        val result = adminService.getToilet(30L)

        assertThat(result.id).isEqualTo(30L)
    }

    @Test
    fun `getToilet - 존재하지 않는 id는 TOILET_NOT_FOUND 예외를 던진다`() {
        `when`(toiletRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy { adminService.getToilet(999L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_NOT_FOUND)
    }

    @Test
    fun `updateToilet - 지정된 필드만 수정된다`() {
        val entity = makeToiletEntity(id = 40L)
        val request = com.geuphalttaen.domain.toilet.AdminToiletUpdateRequest(name = "수정된 화장실")
        `when`(toiletRepository.findById(40L)).thenReturn(entity)

        val result = adminService.updateToilet(40L, request)

        assertThat(result.name).isEqualTo("수정된 화장실")
    }

    @Test
    fun `updateToilet - 존재하지 않는 id는 TOILET_NOT_FOUND 예외를 던진다`() {
        `when`(toiletRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy {
            adminService.updateToilet(
                999L,
                com.geuphalttaen.domain.toilet.AdminToiletUpdateRequest(name = "수정"),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_NOT_FOUND)
    }

    @Test
    fun `deleteToilet - 존재하는 화장실을 삭제한다`() {
        val entity = makeToiletEntity(id = 50L)
        `when`(toiletRepository.findById(50L)).thenReturn(entity)

        adminService.deleteToilet(50L)

        verify(toiletRepository).delete(entity)
    }

    @Test
    fun `deleteToilet - 존재하지 않는 id는 TOILET_NOT_FOUND 예외를 던진다`() {
        `when`(toiletRepository.findById(999L)).thenReturn(null)

        assertThatThrownBy { adminService.deleteToilet(999L) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.TOILET_NOT_FOUND)
    }

    // ────────────────────────────────────────────
    // seedAdmin
    // ────────────────────────────────────────────

    @Test
    fun `seedAdmin - 시크릿이 맞고 관리자가 없으면 계정을 생성한다`() {
        val request = AdminSeedRequest(email = "seed@test.com", password = "password123")
        `when`(adminRepository.existsAny()).thenReturn(false)
        `when`(adminRepository.save(anyNonNull(AdminEntity::class.java)))
            .thenAnswer { it.arguments[0] as AdminEntity }

        adminService.seedAdmin(testSeedSecret, request)

        verify(adminRepository).save(anyNonNull(AdminEntity::class.java))
    }

    @Test
    fun `seedAdmin - 잘못된 시크릿이면 ADMIN_SEED_FORBIDDEN 예외를 던진다`() {
        val request = AdminSeedRequest(email = "seed@test.com", password = "password123")

        assertThatThrownBy { adminService.seedAdmin("wrong-secret", request) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.ADMIN_SEED_FORBIDDEN)
    }

    @Test
    fun `seedAdmin - 이미 관리자가 존재하면 ADMIN_ALREADY_EXISTS 예외를 던진다`() {
        val request = AdminSeedRequest(email = "seed@test.com", password = "password123")
        `when`(adminRepository.existsAny()).thenReturn(true)

        assertThatThrownBy { adminService.seedAdmin(testSeedSecret, request) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.ADMIN_ALREADY_EXISTS)
    }

    // ────────────────────────────────────────────
    // 헬퍼
    // ────────────────────────────────────────────

    private fun makeToiletEntity(
        id: Long = 1L,
        status: ToiletStatus = ToiletStatus.PENDING,
    ): ToiletEntity = ToiletEntity(
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
        status = status,
    )
}
