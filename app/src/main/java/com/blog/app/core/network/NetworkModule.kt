package com.blog.app.core.network

import com.blog.app.core.auth.AuthSessionManager
import com.blog.app.core.config.ApiConfig
import com.blog.app.core.storage.AuthStorage
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
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = AuthStorage.accessToken()
        val authenticatedRequest = request.newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()

        val response = chain.proceed(authenticatedRequest)
        if (response.code != 401 || AuthSessionManager.isRetryRequest(request)) {
            return@Interceptor response
        }

        response.close()

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

    private val authRetrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.AUTH_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    fun <T> create(service: Class<T>): T = retrofit.create(service)

    fun <T> createAuth(service: Class<T>): T = authRetrofit.create(service)
}
