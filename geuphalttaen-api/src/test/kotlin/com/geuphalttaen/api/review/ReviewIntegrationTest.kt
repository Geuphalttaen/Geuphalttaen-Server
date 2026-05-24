package com.geuphalttaen.api.review

import com.fasterxml.jackson.databind.ObjectMapper
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.auth.JwtProvider
import com.geuphalttaen.domain.auth.RefreshTokenRepository
import com.geuphalttaen.domain.image.ImageStoragePort
import com.geuphalttaen.infra.toilet.ToiletJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * SC-07 리뷰 작성 / SC-08 리뷰 목록 조회 / SC-09 청결도 평가 / SC-10 관리자 리뷰 삭제
 * 통합 테스트 — Testcontainers MySQL + MockMvc
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ReviewIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("geuphalttaen_test")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureDataSource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Autowired
    private lateinit var toiletJpaRepository: ToiletJpaRepository

    // RedisConnectionFactory 모킹 — 테스트에서 Redis 서버 불필요
    @MockBean
    private lateinit var redisConnectionFactory: RedisConnectionFactory

    // RefreshTokenRepository 모킹
    @MockBean
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    // Cloudflare R2 @PostConstruct 검증 우회 — 테스트에서 스토리지 불필요
    @MockBean
    private lateinit var imageStoragePort: ImageStoragePort

    private lateinit var userToken: String       // 일반 사용자 (userId = 1)
    private lateinit var anotherUserToken: String // 다른 사용자 (userId = 2)
    private lateinit var adminToken: String       // 관리자
    private var toiletId: Long = 0L

    @BeforeEach
    fun setUp() {
        // DB 초기화
        toiletJpaRepository.deleteAll()

        // JWT 토큰 발급
        userToken = jwtProvider.generateAccessToken(1L)
        anotherUserToken = jwtProvider.generateAccessToken(2L)
        adminToken = jwtProvider.generateAdminAccessToken(99L)

        // 테스트용 화장실 저장
        val toilet = toiletJpaRepository.save(
            ToiletEntity(
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
        )
        toiletId = toilet.id
    }

    // ──────────────────────────────────────────────────────────────────────
    // SC-07. 리뷰 작성
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `SC-07-1 유효한 JWT와 존재하는 toiletId로 리뷰 작성 성공`() {
        val request = mapOf("rating" to 4, "content" to "깨끗해요")

        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/reviews")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.toiletId").value(toiletId))
            .andExpect(jsonPath("$.data.rating").value(4))
            .andExpect(jsonPath("$.data.content").value("깨끗해요"))
    }

    @Test
    fun `SC-07-2 동일 사용자가 동일 화장실에 두 번 리뷰 작성 시 REVIEW_DUPLICATE 반환`() {
        val request = mapOf("rating" to 4, "content" to "깨끗해요")

        // 첫 번째 리뷰 작성
        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/reviews")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk)

        // 두 번째 리뷰 작성 — 중복
        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/reviews")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("RV002"))
    }

    @Test
    fun `SC-07-3 존재하지 않는 toiletId로 리뷰 작성 시 404 TOILET_NOT_FOUND 반환`() {
        val request = mapOf("rating" to 4, "content" to "테스트")
        val nonExistentId = 99999L

        mockMvc.perform(
            post("/api/v1/toilets/$nonExistentId/reviews")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("T001"))
    }

    @Test
    fun `SC-07-4 rating이 0이면 400 Bad Request 반환 (Bean Validation)`() {
        val request = mapOf("rating" to 0)

        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/reviews")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `SC-07-5 rating이 6이면 400 Bad Request 반환 (Bean Validation)`() {
        val request = mapOf("rating" to 6)

        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/reviews")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `SC-07-6 content가 201자이면 400 Bad Request 반환 (Bean Validation)`() {
        val longContent = "a".repeat(201)
        val request = mapOf("rating" to 4, "content" to longContent)

        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/reviews")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `SC-07-7 인증 없이 리뷰 작성 시 401 Unauthorized 반환`() {
        val request = mapOf("rating" to 4, "content" to "테스트")

        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    // ──────────────────────────────────────────────────────────────────────
    // SC-08. 리뷰 목록 조회
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `SC-08-1 인증 없이 리뷰 목록 조회 성공`() {
        // 먼저 리뷰 하나 작성
        val request = mapOf("rating" to 4, "content" to "깨끗해요")
        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/reviews")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk)

        // 인증 없이 목록 조회
        mockMvc.perform(
            get("/api/v1/toilets/$toiletId/reviews")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.totalElements").value(1))
    }

    @Test
    fun `SC-08-2 존재하지 않는 toiletId로 리뷰 목록 조회 시 404 TOILET_NOT_FOUND 반환`() {
        mockMvc.perform(
            get("/api/v1/toilets/99999/reviews")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("T001"))
    }

    // ──────────────────────────────────────────────────────────────────────
    // SC-09. 청결도 평가
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `SC-09-1 청결도 평가 최초 등록 성공`() {
        val request = mapOf("score" to 3)

        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/cleanliness")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.toiletId").value(toiletId))
            .andExpect(jsonPath("$.data.score").value(3))
    }

    @Test
    fun `SC-09-2 청결도 평가 재등록 시 upsert 동작 — 점수가 업데이트된다`() {
        // 첫 번째 등록
        val firstRequest = mapOf("score" to 3)
        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/cleanliness")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.score").value(3))

        // 두 번째 등록 — score 업데이트
        val secondRequest = mapOf("score" to 5)
        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/cleanliness")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.score").value(5))
    }

    @Test
    fun `SC-09-3 score가 0이면 400 Bad Request 반환 (Bean Validation)`() {
        val request = mapOf("score" to 0)

        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/cleanliness")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `SC-09-4 score가 6이면 400 Bad Request 반환 (Bean Validation)`() {
        val request = mapOf("score" to 6)

        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/cleanliness")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `SC-09-5 인증 없이 청결도 평가 시 401 Unauthorized 반환`() {
        val request = mapOf("score" to 3)

        mockMvc.perform(
            post("/api/v1/toilets/$toiletId/cleanliness")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    // ──────────────────────────────────────────────────────────────────────
    // SC-10. 관리자 리뷰 삭제
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `SC-10-1 관리자가 존재하는 리뷰를 삭제하면 204 No Content 반환`() {
        // 리뷰 먼저 작성
        val reviewId = createReview(userId = 1L, rating = 4)

        mockMvc.perform(
            delete("/api/v1/admin/reviews/$reviewId")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `SC-10-2 관리자가 존재하지 않는 리뷰 삭제 시 404 REVIEW_NOT_FOUND 반환`() {
        mockMvc.perform(
            delete("/api/v1/admin/reviews/99999")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("RV001"))
    }

    @Test
    fun `SC-10-3 일반 사용자 JWT로 리뷰 삭제 시 403 Forbidden 반환`() {
        val reviewId = createReview(userId = 2L, rating = 3)

        mockMvc.perform(
            delete("/api/v1/admin/reviews/$reviewId")
                .header("Authorization", "Bearer $anotherUserToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `SC-10-4 인증 없이 리뷰 삭제 시 401 Unauthorized 반환`() {
        val reviewId = createReview(userId = 1L, rating = 4)

        mockMvc.perform(
            delete("/api/v1/admin/reviews/$reviewId")
        )
            .andExpect(status().isUnauthorized)
    }

    // ──────────────────────────────────────────────────────────────────────
    // SC-10 추가: 관리자 리뷰 목록 조회 (인증 필요)
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `관리자 리뷰 목록 조회 — 관리자 JWT로 200 OK 반환`() {
        createReview(userId = 1L, rating = 5)

        mockMvc.perform(
            get("/api/v1/admin/toilets/$toiletId/reviews")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    fun `관리자 리뷰 목록 조회 — 일반 사용자 JWT로 403 Forbidden 반환`() {
        mockMvc.perform(
            get("/api/v1/admin/toilets/$toiletId/reviews")
                .header("Authorization", "Bearer $userToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `관리자 리뷰 목록 조회 — 인증 없이 401 Unauthorized 반환`() {
        mockMvc.perform(
            get("/api/v1/admin/toilets/$toiletId/reviews")
        )
            .andExpect(status().isUnauthorized)
    }

    // ──────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 지정한 사용자로 리뷰를 작성하고, 생성된 리뷰 ID를 반환한다.
     */
    private fun createReview(userId: Long, rating: Int, content: String = "테스트 리뷰"): Long {
        val token = jwtProvider.generateAccessToken(userId)
        val request = mapOf("rating" to rating, "content" to content)

        val result = mockMvc.perform(
            post("/api/v1/toilets/$toiletId/reviews")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andReturn()

        val response = objectMapper.readTree(result.response.contentAsString)
        return response["data"]["id"].asLong()
    }
}
