package com.geuphalttaen.infra.user

import com.geuphalttaen.core.entity.OAuthProvider
import com.geuphalttaen.core.entity.UserEntity
import com.geuphalttaen.domain.auth.UserRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {

    override fun findByProviderAndProviderId(provider: String, providerId: String): UserEntity? {
        val oAuthProvider = OAuthProvider.valueOf(provider.uppercase())
        return jpaRepository.findByProviderAndProviderId(oAuthProvider, providerId)
    }

    override fun findById(id: Long): UserEntity? = jpaRepository.findById(id).orElse(null)

    override fun findAllByIds(ids: List<Long>): List<UserEntity> = jpaRepository.findAllByIdIn(ids)

    override fun save(entity: UserEntity): UserEntity = jpaRepository.save(entity)

    override fun delete(entity: UserEntity) = jpaRepository.delete(entity)
}
