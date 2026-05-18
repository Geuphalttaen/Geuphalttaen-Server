package com.geuphalttaen.infra.user

import com.geuphalttaen.core.entity.OAuthProvider
import com.geuphalttaen.core.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun findByProviderAndProviderId(provider: OAuthProvider, providerId: String): UserEntity?
}
