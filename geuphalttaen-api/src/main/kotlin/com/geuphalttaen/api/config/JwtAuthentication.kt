package com.geuphalttaen.api.config

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority

class JwtAuthentication(
    private val userId: Long,
) : Authentication {
    private var authenticated = true

    override fun getName(): String = userId.toString()
    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()
    override fun getCredentials(): Any? = null
    override fun getDetails(): Any? = null
    override fun getPrincipal(): Long = userId
    override fun isAuthenticated(): Boolean = authenticated
    override fun setAuthenticated(isAuthenticated: Boolean) {
        authenticated = isAuthenticated
    }
}
