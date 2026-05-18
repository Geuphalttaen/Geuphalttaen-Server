package com.geuphalttaen.domain.auth

data class OAuthLoginRequest(
    val provider: String,
    val accessToken: String,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

data class RefreshRequest(
    val refreshToken: String,
)
