package com.geuphalttaen.domain.auth

import com.geuphalttaen.core.entity.OAuthProvider

data class OAuthLoginRequest(
    val provider: OAuthProvider,
    val accessToken: String,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
