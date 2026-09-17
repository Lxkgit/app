package com.blog.app.data.repository

import com.blog.app.core.config.ApiConfig
import com.blog.app.core.network.NetworkModule
import com.blog.app.core.storage.AuthStorage
import com.blog.app.data.api.AuthApi

/**
 * Handles authentication with the existing blog OAuth2 service.
 */
class AuthRepository(
    private val api: AuthApi = NetworkModule.createAuth(AuthApi::class.java)
) {
    /**
     * Requests an access token using the blog's existing password grant.
     */
    suspend fun login(username: String, password: String) {
        val token = api.login(
            grantType = "password",
            clientId = ApiConfig.OAUTH_CLIENT_ID,
            clientSecret = ApiConfig.OAUTH_CLIENT_SECRET,
            username = username,
            password = password
        )
        AuthStorage.saveLogin(
            username = username,
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            expiresIn = token.expiresIn
        )
    }

    /**
     * Clears the local login state.
     */
    fun logout() {
        AuthStorage.clear()
    }
}
