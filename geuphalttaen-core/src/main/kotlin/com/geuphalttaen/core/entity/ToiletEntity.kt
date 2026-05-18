package com.geuphalttaen.core.entity

import jakarta.persistence.*

enum class ToiletStatus {
    ACTIVE,
    PENDING,
    REJECTED,
}

@Entity
@Table(name = "toilets")
class ToiletEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var address: String,

    @Column(nullable = false)
    var lat: Double,

    @Column(nullable = false)
    var lng: Double,

    @Column(name = "is_public", nullable = false)
    var isPublic: Boolean = true,

    @Column(nullable = false)
    var male: Boolean = true,

    @Column(nullable = false)
    var female: Boolean = true,

    @Column(nullable = false)
    var disabled: Boolean = false,

    @Column(name = "reported_by")
    var reportedBy: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ToiletStatus = ToiletStatus.PENDING,

) : BaseEntity()
