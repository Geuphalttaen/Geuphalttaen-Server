package com.geuphalttaen.domain.auth

import com.geuphalttaen.core.entity.UserEntity

interface UserRepository {
    fun findByProviderAndProviderId(provider: String, providerId: String): UserEntity?
    fun save(entity: UserEntity): UserEntity
}
