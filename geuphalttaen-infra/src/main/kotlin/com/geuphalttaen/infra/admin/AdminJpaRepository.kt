package com.geuphalttaen.infra.admin

import com.geuphalttaen.core.entity.AdminEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 관리자 JPA 레포지토리.
 */
interface AdminJpaRepository : JpaRepository<AdminEntity, Long> {
    fun findByEmail(email: String): AdminEntity?
}
