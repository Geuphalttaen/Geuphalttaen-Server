package com.geuphalttaen.infra.auth

import com.geuphalttaen.domain.auth.RefreshTokenRepository
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

@Repository
class RedisRefreshTokenRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) : RefreshTokenRepository {

    private companion object {
        const val KEY_PREFIX = "refresh_token:"
        const val TTL_DAYS = 14L
    }

    override fun save(userId: Long, refreshToken: String) {
        redisTemplate.opsForValue().set(
            buildKey(userId),
            refreshToken,
            TTL_DAYS,
            TimeUnit.DAYS,
        )
    }

    override fun find(userId: Long): String? =
        redisTemplate.opsForValue().get(buildKey(userId))

    override fun delete(userId: Long) {
        redisTemplate.delete(buildKey(userId))
    }

    private fun buildKey(userId: Long): String = "$KEY_PREFIX$userId"
}
