package com.geuphalttaen.domain.admin

import com.geuphalttaen.core.entity.AdminEntity

/**
 * 관리자 계정 저장소 포트.
 */
interface AdminRepository {
    fun findByEmail(email: String): AdminEntity?
    fun existsAny(): Boolean
    fun save(entity: AdminEntity): AdminEntity
}
