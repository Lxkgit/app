package com.blog.app.data.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OAuth2 token response returned by the blog authorization service.
 */
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long = 0L,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val scope: String? = null
)
