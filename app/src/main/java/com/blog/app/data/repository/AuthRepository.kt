package com.blog.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.blog.app.core.config.ApiConfig
import com.blog.app.core.storage.AuthStorage
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import org.json.JSONObject

/**
 * Handles OAuth2 authorization code and PKCE authentication with the blog service.
 */
class AuthRepository {
    private fun serviceConfiguration(): AuthorizationServiceConfiguration {
        return AuthorizationServiceConfiguration(
            Uri.parse(ApiConfig.OAUTH_AUTHORIZATION_ENDPOINT),
            Uri.parse(ApiConfig.OAUTH_TOKEN_ENDPOINT)
        )
    }

    /**
     * Creates the browser authorization intent for the Android public client.
     */
    fun authorizationIntent(context: Context): Intent {
        val request = AuthorizationRequest.Builder(
            serviceConfiguration(),
            ApiConfig.OAUTH_CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(ApiConfig.OAUTH_REDIRECT_URI)
        )
            .setScope("openid")
            .build()

        return AuthorizationService(context).getAuthorizationRequestIntent(request)
    }

    /**
     * Exchanges the authorization code and persists the authenticated state.
     */
    fun handleAuthorizationResponse(
        context: Context,
        intent: Intent,
        onResult: (Result<Unit>) -> Unit
    ) {
        val response = AuthorizationResponse.fromIntent(intent)
        val exception = AuthorizationException.fromIntent(intent)

        if (response == null) {
            onResult(Result.failure(exception ?: IllegalStateException("登录授权失败")))
            return
        }

        val authorizationService = AuthorizationService(context)
        authorizationService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenException ->
            try {
                if (tokenResponse == null) {
                    onResult(Result.failure(tokenException ?: IllegalStateException("获取登录令牌失败")))
                    return@performTokenRequest
                }

                val authState = AuthState(serviceConfiguration())
                authState.update(response, tokenResponse, tokenException)

                val username = readUsername(tokenResponse) ?: response.clientId
                AuthStorage.saveLogin(
                    username = username,
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresIn = tokenResponse.expiresIn,
                    authState = authState.jsonSerializeString()
                )
                onResult(Result.success(Unit))
            } finally {
                authorizationService.dispose()
            }
        }
    }

    /**
     * Clears the local login state.
     */
    fun logout() {
        AuthStorage.clear()
    }

    /**
     * Reads the username claim that the blog authorization server adds to the ID token.
     */
    private fun readUsername(tokenResponse: TokenResponse): String? {
        val idToken = tokenResponse.idToken ?: return null
        return runCatching {
            val payload = idToken.split(".")[1]
            val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP))
            JSONObject(json).optString("username").takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
