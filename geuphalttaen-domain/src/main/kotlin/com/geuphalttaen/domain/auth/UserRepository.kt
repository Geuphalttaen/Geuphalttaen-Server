package com.geuphalttaen.domain.auth

import com.geuphalttaen.core.entity.UserEntity

interface UserRepository {
    fun findByProviderAndProviderId(provider: String, providerId: String): UserEntity?
    fun findById(id: Long): UserEntity?
    fun findAllByIds(ids: List<Long>): List<UserEntity>
    fun save(entity: UserEntity): UserEntity
}
