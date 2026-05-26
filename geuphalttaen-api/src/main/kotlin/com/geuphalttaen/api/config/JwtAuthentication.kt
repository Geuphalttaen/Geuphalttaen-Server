package com.geuphalttaen.api.config

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * JWT 기반 인증 객체.
 * roles 목록을 GrantedAuthority로 변환하여 Spring Security 권한 체계와 연동한다.
 */
class JwtAuthentication(
    private val userId: Long,
    private val roles: List<String> = emptyList(),
) : Authentication {
    private var authenticated = true

    override fun getName(): String = userId.toString()
    override fun getAuthorities(): Collection<GrantedAuthority> =
        roles.map { SimpleGrantedAuthority(it) }
    override fun getCredentials(): Any? = null
    override fun getDetails(): Any? = null
    override fun getPrincipal(): Long = userId
    override fun isAuthenticated(): Boolean = authenticated
    override fun setAuthenticated(isAuthenticated: Boolean) {
        authenticated = isAuthenticated
    }
}
