package com.blog.app.core.storage

import android.content.Context

/**
 * 在设备本地保存 OAuth2 登录状态。
 */
object AuthStorage {
    private const val PREFS_NAME = "blog_auth"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"
    private const val EXPIRES_AT = "expires_at"
    private const val USERNAME = "username"
    private const val AUTH_STATE = "auth_state"

    private var preferences: android.content.SharedPreferences? = null
    private var applicationContext: Context? = null

    /**
     * 初始化本地持久化存储。
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        preferences = applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 获取应用上下文。
     */
    fun context(): Context? = applicationContext

    /**
     * 获取当前访问令牌。
     */
    fun accessToken(): String? = preferences?.getString(ACCESS_TOKEN, null)

    /**
     * 获取保存的刷新令牌。
     */
    fun refreshToken(): String? = preferences?.getString(REFRESH_TOKEN, null)

    /**
     * 判断当前是否存在未过期的访问令牌。
     */
    fun isLoggedIn(): Boolean = !accessToken().isNullOrBlank() && expiresAt() > System.currentTimeMillis()

    /**
     * 获取保存的登录用户名。
     */
    fun username(): String = preferences?.getString(USERNAME, "").orEmpty()

    /**
     * 获取访问令牌的过期时间戳，单位为毫秒。
     */
    fun expiresAt(): Long = preferences?.getLong(EXPIRES_AT, 0L) ?: 0L

    /**
     * 获取保存的 AppAuth 授权状态。
     */
    fun authState(): String? = preferences?.getString(AUTH_STATE, null)

    /**
     * 保存成功的 OAuth2 登录状态。
     */
    fun saveLogin(
        username: String,
        accessToken: String,
        refreshToken: String?,
        expiresIn: Long,
        authState: String
    ) {
        preferences?.edit()
            ?.putString(USERNAME, username)
            ?.putString(ACCESS_TOKEN, accessToken)
            ?.putString(REFRESH_TOKEN, refreshToken)
            ?.putLong(EXPIRES_AT, System.currentTimeMillis() + expiresIn * 1000L)
            ?.putString(AUTH_STATE, authState)
            ?.apply()
    }

    /**
     * 清除本地登录状态。
     */
    fun clear() {
        preferences?.edit()?.clear()?.apply()
    }
}
