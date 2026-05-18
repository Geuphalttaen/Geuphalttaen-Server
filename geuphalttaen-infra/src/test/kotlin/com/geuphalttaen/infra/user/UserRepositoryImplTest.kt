package com.geuphalttaen.infra.user

import com.geuphalttaen.core.entity.OAuthProvider
import com.geuphalttaen.core.entity.UserEntity
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
@Import(UserRepositoryImpl::class)
class UserRepositoryImplTest {

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
    private lateinit var userRepository: UserRepositoryImpl

    @Test
    fun `save - UserEntity를 저장하면 id가 부여된다`() {
        val user = UserEntity(
            provider = OAuthProvider.KAKAO,
            providerId = "kakao-001",
            nickname = "테스터",
        )

        val saved = userRepository.save(user)

        assertThat(saved.id).isGreaterThan(0L)
        assertThat(saved.provider).isEqualTo(OAuthProvider.KAKAO)
        assertThat(saved.providerId).isEqualTo("kakao-001")
    }

    @Test
    fun `findByProviderAndProviderId - 저장된 사용자를 provider와 providerId로 조회한다`() {
        val user = UserEntity(
            provider = OAuthProvider.APPLE,
            providerId = "apple-xyz-999",
            nickname = "애플유저",
        )
        userRepository.save(user)

        val found = userRepository.findByProviderAndProviderId("APPLE", "apple-xyz-999")

        assertThat(found).isNotNull
        assertThat(found!!.nickname).isEqualTo("애플유저")
    }

    @Test
    fun `findByProviderAndProviderId - 존재하지 않는 사용자는 null을 반환한다`() {
        val result = userRepository.findByProviderAndProviderId("KAKAO", "nonexistent-id")

        assertThat(result).isNull()
    }

    @Test
    fun `findByProviderAndProviderId - provider가 달라도 providerId가 같으면 null을 반환한다`() {
        val user = UserEntity(
            provider = OAuthProvider.KAKAO,
            providerId = "shared-id-001",
            nickname = "카카오유저",
        )
        userRepository.save(user)

        // APPLE provider로 같은 providerId를 조회 → 다른 레코드이므로 null
        val result = userRepository.findByProviderAndProviderId("APPLE", "shared-id-001")

        assertThat(result).isNull()
    }
}
