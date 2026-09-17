package com.blog.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.blog.app.core.config.ApiConfig
import com.blog.app.core.storage.AuthStorage
import com.blog.app.debug.DebugLog
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.CodeVerifierUtil
import net.openid.appauth.TokenRequest
import net.openid.appauth.ResponseTypeValues
import org.json.JSONObject
import java.util.UUID

/**
 * 处理博客服务的 OAuth2 授权码和 PKCE 登录流程。
 */
class AuthRepository {
    private fun serviceConfiguration(): AuthorizationServiceConfiguration {
        return AuthorizationServiceConfiguration(
            Uri.parse(ApiConfig.OAUTH_AUTHORIZATION_ENDPOINT),
            Uri.parse(ApiConfig.OAUTH_TOKEN_ENDPOINT)
        )
    }

    /**
     * 创建应用内登录页面需要加载的授权地址。
     */
    fun createAuthorizationRequest(): AuthorizationRequest {
        val codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier()
        val state = UUID.randomUUID().toString()

        return AuthorizationRequest.Builder(
            serviceConfiguration(),
            ApiConfig.OAUTH_CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(ApiConfig.OAUTH_REDIRECT_URI)
        )
            .setScope("openid")
            .setState(state)
            .setCodeVerifier(codeVerifier)
            .build()
    }

    /**
     * 使用授权码和 PKCE 验证码换取访问令牌，并保存登录状态。
     */
    fun exchangeAuthorizationCode(
        context: Context,
        request: AuthorizationRequest,
        callbackUri: Uri,
        onResult: (Result<Unit>) -> Unit
    ) {
        val code = callbackUri.getQueryParameter("code")
        val state = callbackUri.getQueryParameter("state")

        if (code.isNullOrBlank()) {
            val error = callbackUri.getQueryParameter("error_description")
                ?: callbackUri.getQueryParameter("error")
                ?: "登录授权失败"
            DebugLog.add("OAuth2 回调缺少 authorization code: $error")
            Log.e(TAG, "OAuth2 回调没有 authorization code: $error")
            onResult(Result.failure(IllegalStateException(error)))
            return
        }

        if (request.state.isNullOrBlank() || request.state != state) {
            DebugLog.add("OAuth2 state 校验失败")
            Log.e(TAG, "OAuth2 state 校验失败")
            onResult(Result.failure(IllegalStateException("登录状态校验失败")))
            return
        }

        val authorizationResponse = AuthorizationResponse.Builder(request)
            .fromUri(callbackUri)
            .build()

        if (authorizationResponse.authorizationCode.isNullOrBlank()) {
            DebugLog.add("OAuth2 authorization response 无效")
            Log.e(TAG, "OAuth2 authorization response 无效")
            onResult(Result.failure(IllegalStateException("登录授权响应无效")))
            return
        }

        val tokenRequest = TokenRequest.Builder(
            serviceConfiguration(),
            ApiConfig.OAUTH_CLIENT_ID
        )
            .setGrantType("authorization_code")
            .setAuthorizationCode(code)
            .setRedirectUri(Uri.parse(ApiConfig.OAUTH_REDIRECT_URI))
            .setCodeVerifier(request.codeVerifier)
            .build()

        DebugLog.add("POST ${ApiConfig.OAUTH_TOKEN_ENDPOINT}")
        DebugLog.add("OAuth2 token 请求参数: grant_type=authorization_code, client_id=${ApiConfig.OAUTH_CLIENT_ID}, PKCE=已提供")
        Log.d(TAG, "开始请求 token endpoint: ${ApiConfig.OAUTH_TOKEN_ENDPOINT}")
        val authorizationService = AuthorizationService(context)
        authorizationService.performTokenRequest(tokenRequest) { tokenResponse, tokenException ->
            try {
                if (tokenResponse == null) {
                    DebugLog.add("OAuth2 token 响应失败: ${tokenException?.javaClass?.simpleName ?: "未知错误"}: ${tokenException?.message ?: "无响应"}")
                    Log.e(TAG, "token endpoint 请求失败", tokenException)
                    onResult(Result.failure(tokenException ?: IllegalStateException("获取登录令牌失败")))
                    return@performTokenRequest
                }

                val accessToken = tokenResponse.accessToken
                if (accessToken.isNullOrBlank()) {
                    DebugLog.add("OAuth2 token 响应异常: 未返回 access_token")
                    Log.e(TAG, "token endpoint 没有返回 access token")
                    onResult(Result.failure(IllegalStateException("登录服务未返回 access token")))
                    return@performTokenRequest
                }

                val username = readUsername(tokenResponse) ?: ApiConfig.OAUTH_CLIENT_ID
                val expiresIn = tokenResponse.accessTokenExpirationTime?.let { expirationTime ->
                    ((expirationTime - System.currentTimeMillis()).coerceAtLeast(0L) / 1000L)
                } ?: 0L

                val authState = AuthState(serviceConfiguration())
                authState.update(authorizationResponse, tokenException)
                authState.update(tokenResponse, tokenException)

                AuthStorage.saveLogin(
                    username = username,
                    accessToken = accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresIn = expiresIn,
                    authState = authState.jsonSerializeString()
                )
                DebugLog.add("OAuth2 token 响应成功: HTTP 请求完成，expiresIn=${expiresIn}s")
                DebugLog.add("登录状态已保存: username=$username")
                Log.d(TAG, "OAuth2 token 获取成功，username=$username, expiresIn=${expiresIn}s")
                onResult(Result.success(Unit))
            } finally {
                authorizationService.dispose()
            }
        }
    }

    /**
     * 清除本地登录状态。
     */
    fun logout() {
        AuthStorage.clear()
    }

    /**
     * 读取授权服务器写入 ID Token 的用户名声明。
     */
    private fun readUsername(tokenResponse: net.openid.appauth.TokenResponse): String? {
        val idToken = tokenResponse.idToken ?: return null
        return runCatching {
            val payload = idToken.split(".")[1]
            val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP))
            JSONObject(json).optString("username").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "BLOG_OAUTH"
    }
}
