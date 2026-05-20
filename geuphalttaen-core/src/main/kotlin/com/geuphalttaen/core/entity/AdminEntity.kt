package com.geuphalttaen.core.entity

import jakarta.persistence.*

/**
 * 관리자 계정 엔티티.
 * 이메일과 BCrypt 해시 패스워드를 저장한다.
 */
@Entity
@Table(
    name = "admins",
    uniqueConstraints = [UniqueConstraint(columnNames = ["email"])],
)
class AdminEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

) : BaseEntity()
