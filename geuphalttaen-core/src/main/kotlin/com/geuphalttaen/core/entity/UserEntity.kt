package com.geuphalttaen.core.entity

import jakarta.persistence.*

enum class OAuthProvider {
    KAKAO,
    APPLE,
}

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_id"])],
)
class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var provider: OAuthProvider,

    @Column(name = "provider_id", nullable = false)
    var providerId: String,

    @Column(nullable = false)
    var nickname: String,

) : BaseEntity()
