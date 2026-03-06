package com.geuphalttaen.server.domain

import jakarta.persistence.*
import java.time.LocalTime

@Entity
@Table(name = "toilet")
class Toilet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val roadAddress: String,

    @Column(nullable = false)
    val address: String,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double,

    val maleToiletCount: Int = 0,
    val femaleToiletCount: Int = 0,
    val disabledMaleToiletCount: Int = 0,
    val disabledFemaleToiletCount: Int = 0,

    val openTime: LocalTime? = null,
    val closeTime: LocalTime? = null,

    @Column(nullable = false)
    val isDisabledAvailable: Boolean = false,

    val managementAgency: String? = null,
    val phoneNumber: String? = null,
)
