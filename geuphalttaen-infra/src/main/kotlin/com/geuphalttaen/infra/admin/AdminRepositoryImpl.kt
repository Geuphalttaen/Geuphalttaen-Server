package com.geuphalttaen.infra.admin

import com.geuphalttaen.core.entity.AdminEntity
import com.geuphalttaen.domain.admin.AdminRepository
import org.springframework.stereotype.Repository

/**
 * AdminRepository 구현체.
 */
@Repository
class AdminRepositoryImpl(
    private val jpaRepository: AdminJpaRepository,
) : AdminRepository {

    override fun findByEmail(email: String): AdminEntity? = jpaRepository.findByEmail(email)

    override fun existsAny(): Boolean = jpaRepository.count() > 0

    override fun save(entity: AdminEntity): AdminEntity = jpaRepository.save(entity)
}
