package com.geuphalttaen.core.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "reviews",
    indexes = [Index(name = "idx_reviews_toilet_id", columnList = "toilet_id")],
    uniqueConstraints = [UniqueConstraint(name = "uk_reviews_toilet_user", columnNames = ["toilet_id", "user_id"])],
)
class ReviewEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(name = "toilet_id", nullable = false) val toiletId: Long,
    @Column(name = "user_id", nullable = false) val userId: Long,
    @Column(nullable = false) val rating: Int,
    @Column(length = 200) val content: String? = null,
) : BaseEntity()
