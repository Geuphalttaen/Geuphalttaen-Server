package com.geuphalttaen.domain.auth

import jakarta.validation.constraints.NotBlank

data class OAuthLoginRequest(
    @field:NotBlank val provider: String,
    @field:NotBlank val accessToken: String,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

data class RefreshRequest(
    @field:NotBlank val refreshToken: String,
)
