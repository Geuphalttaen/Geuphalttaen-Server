package com.geuphalttaen.infra.toilet

import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ToiletRepositoryImpl::class)
class ToiletRepositoryImplTest {

    companion object {
        @Container
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
    private lateinit var toiletRepository: ToiletRepositoryImpl

    // ──────────────────────────────────────────
    // save
    // ──────────────────────────────────────────

    @Test
    fun `save - ToiletEntity를 저장하면 id가 부여된다`() {
        val entity = makeToiletEntity()

        val saved = toiletRepository.save(entity)

        assertThat(saved.id).isGreaterThan(0L)
        assertThat(saved.name).isEqualTo("테스트 화장실")
        assertThat(saved.familyRoom).isFalse()
    }

    @Test
    fun `save - familyRoom이 true인 화장실을 저장하고 조회할 수 있다`() {
        val entity = makeToiletEntity(familyRoom = true)

        val saved = toiletRepository.save(entity)
        val found = toiletRepository.findById(saved.id)

        assertThat(found).isNotNull
        assertThat(found!!.familyRoom).isTrue()
    }

    // ──────────────────────────────────────────
    // findById
    // ──────────────────────────────────────────

    @Test
    fun `findById - 저장된 화장실을 id로 조회한다`() {
        val entity = makeToiletEntity(status = ToiletStatus.ACTIVE)
        val saved = toiletRepository.save(entity)

        val found = toiletRepository.findById(saved.id)

        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(saved.id)
        assertThat(found.name).isEqualTo("테스트 화장실")
    }

    @Test
    fun `findById - 존재하지 않는 id로 조회하면 null을 반환한다`() {
        val result = toiletRepository.findById(Long.MAX_VALUE)

        assertThat(result).isNull()
    }

    // ──────────────────────────────────────────
    // findNearby
    // ──────────────────────────────────────────

    @Test
    fun `findNearby - ACTIVE 상태의 화장실만 반환한다`() {
        // ACTIVE 화장실 저장
        val active = makeToiletEntity(status = ToiletStatus.ACTIVE)
        toiletRepository.save(active)

        // PENDING 화장실 저장 (반환 안 돼야 함)
        val pending = makeToiletEntity(status = ToiletStatus.PENDING)
        toiletRepository.save(pending)

        // 서울 시청 근방 1km 조회
        val results = toiletRepository.findNearby(
            lat = 37.5665,
            lng = 126.9780,
            radiusMeters = 1000,
        )

        assertThat(results).allMatch { it.status == ToiletStatus.ACTIVE }
    }

    @Test
    fun `findNearby - 반경 바깥 화장실은 반환하지 않는다`() {
        // 부산 (서울에서 약 325km)에 화장실 저장
        val busanToilet = ToiletEntity(
            name = "부산 화장실",
            address = "부산시 중구 1번지",
            lat = 35.1796,
            lng = 129.0756,
            isPublic = true,
            male = true,
            female = true,
            disabled = false,
            familyRoom = false,
            reportedBy = null,
            status = ToiletStatus.ACTIVE,
        )
        toiletRepository.save(busanToilet)

        // 서울 시청 근방 1km 조회
        val results = toiletRepository.findNearby(
            lat = 37.5665,
            lng = 126.9780,
            radiusMeters = 1000,
        )

        assertThat(results).noneMatch { it.name == "부산 화장실" }
    }

    // ──────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────

    private fun makeToiletEntity(
        familyRoom: Boolean = false,
        status: ToiletStatus = ToiletStatus.PENDING,
    ): ToiletEntity = ToiletEntity(
        name = "테스트 화장실",
        address = "서울시 중구 테스트로 1",
        lat = 37.5665,
        lng = 126.9780,
        isPublic = true,
        male = true,
        female = true,
        disabled = false,
        familyRoom = familyRoom,
        reportedBy = null,
        status = status,
    )
}
