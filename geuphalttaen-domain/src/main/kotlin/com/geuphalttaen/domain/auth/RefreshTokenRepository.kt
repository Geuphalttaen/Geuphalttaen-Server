package com.geuphalttaen.domain.auth

interface RefreshTokenRepository {
    fun save(userId: Long, refreshToken: String)
    fun find(userId: Long): String?
    fun delete(userId: Long)
}
