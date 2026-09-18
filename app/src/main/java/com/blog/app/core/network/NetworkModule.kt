package com.blog.app.core.network

import com.blog.app.core.auth.AuthSessionManager
import com.blog.app.core.config.ApiConfig
import com.blog.app.core.storage.AuthStorage
import com.blog.app.data.repository.AuthRepository
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 提供应用使用的 HTTP 客户端和 Retrofit 实例。
 */
object NetworkModule {
    private val refreshLock = Any()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = AuthStorage.accessToken()

        // access_token 已过期时，优先使用 refresh_token 静默刷新，避免用户被强制退出。
        if (!token.isNullOrBlank() && AuthStorage.expiresAt() <= System.currentTimeMillis()) {
            refreshAccessToken(token)
        }

        val currentToken = AuthStorage.accessToken()
        val authenticatedRequest = request.newBuilder().apply {
            if (!currentToken.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $currentToken")
            }
        }.build()

        val response = chain.proceed(authenticatedRequest)
        if (response.code != 401 || AuthSessionManager.isRetryRequest(request)) {
            return@Interceptor response
        }

        response.close()

        // 服务端主动判定 access_token 失效时，也先尝试 refresh_token。
        if (refreshAccessToken(currentToken)) {
            val refreshedToken = AuthStorage.accessToken()
            val retryRequest = AuthSessionManager.markRetry(request).newBuilder().apply {
                removeHeader("Authorization")
                if (!refreshedToken.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer $refreshedToken")
                }
            }.build()
            return@Interceptor chain.proceed(retryRequest)
        }

        // refresh_token 也失效后，才进入原有的重新登录流程。
        if (!AuthSessionManager.waitForReLogin()) {
            val unauthenticatedRetry = AuthSessionManager.markRetry(request)
                .newBuilder()
                .removeHeader("Authorization")
                .build()
            return@Interceptor chain.proceed(unauthenticatedRetry)
        }

        val newToken = AuthStorage.accessToken()
        val retryRequest = AuthSessionManager.markRetry(request).newBuilder().apply {
            removeHeader("Authorization")
            if (!newToken.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $newToken")
            }
        }.build()
        chain.proceed(retryRequest)
    }

    /**
     * 串行刷新 access_token，避免多个并发请求同时消耗 refresh_token。
     */
    private fun refreshAccessToken(failedToken: String?): Boolean {
        synchronized(refreshLock) {
            val currentToken = AuthStorage.accessToken()
            if (!failedToken.isNullOrBlank() && currentToken != failedToken &&
                !currentToken.isNullOrBlank() && AuthStorage.expiresAt() > System.currentTimeMillis()
            ) {
                return true
            }
            return AuthRepository().refreshAccessToken()
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    fun <T> create(service: Class<T>): T = retrofit.create(service)
}
