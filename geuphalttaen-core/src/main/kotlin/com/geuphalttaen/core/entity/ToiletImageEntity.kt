package com.geuphalttaen.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "toilet_images",
    indexes = [Index(name = "idx_toilet_images_toilet_id", columnList = "toilet_id")],
)
class ToiletImageEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "toilet_id", nullable = false)
    val toiletId: Long,

    @Column(nullable = false, length = 500)
    val url: String,

    @Column(name = "original_url", nullable = false, length = 500)
    val originalUrl: String,

) : BaseEntity()
