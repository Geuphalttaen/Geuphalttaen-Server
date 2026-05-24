package com.geuphalttaen.core.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "cleanliness_scores",
    indexes = [Index(name = "idx_cleanliness_toilet_id", columnList = "toilet_id")],
    uniqueConstraints = [UniqueConstraint(name = "uk_cleanliness_toilet_user", columnNames = ["toilet_id", "user_id"])],
)
class CleanlinessEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(name = "toilet_id", nullable = false) val toiletId: Long,
    @Column(name = "user_id", nullable = false) val userId: Long,
    @Column(nullable = false) var score: Int,
) : BaseEntity()
