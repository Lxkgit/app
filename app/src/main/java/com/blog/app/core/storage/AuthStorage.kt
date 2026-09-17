package com.blog.app.core.storage

import android.content.Context

/**
 * Stores the OAuth2 login state locally on the device.
 */
object AuthStorage {
    private const val PREFS_NAME = "blog_auth"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"
    private const val EXPIRES_AT = "expires_at"
    private const val USERNAME = "username"
    private const val AUTH_STATE = "auth_state"

    private var preferences: android.content.SharedPreferences? = null

    /**
     * Initializes the persistent storage.
     */
    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the current access token.
     */
    fun accessToken(): String? = preferences?.getString(ACCESS_TOKEN, null)

    /**
     * Returns the stored refresh token.
     */
    fun refreshToken(): String? = preferences?.getString(REFRESH_TOKEN, null)

    /**
     * Returns whether a token is currently stored and not expired.
     */
    fun isLoggedIn(): Boolean = !accessToken().isNullOrBlank() && expiresAt() > System.currentTimeMillis()

    /**
     * Returns the stored login name.
     */
    fun username(): String = preferences?.getString(USERNAME, "").orEmpty()

    /**
     * Returns the access token expiration timestamp in milliseconds.
     */
    fun expiresAt(): Long = preferences?.getLong(EXPIRES_AT, 0L) ?: 0L

    /**
     * Returns the persisted AppAuth state.
     */
    fun authState(): String? = preferences?.getString(AUTH_STATE, null)

    /**
     * Saves a successful OAuth2 login.
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
     * Clears the local login state.
     */
    fun clear() {
        preferences?.edit()?.clear()?.apply()
    }
}
