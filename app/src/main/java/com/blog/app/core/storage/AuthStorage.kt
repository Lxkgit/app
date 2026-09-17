package com.blog.app.core.storage

import android.content.Context

/**
 * Stores the OAuth2 login state locally on the device.
 */
object AuthStorage {
    private const val PREFS_NAME = "blog_auth"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"
    private const val EXPIRES_IN = "expires_in"
    private const val USERNAME = "username"

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
     * Returns whether a token is currently stored.
     */
    fun isLoggedIn(): Boolean = !accessToken().isNullOrBlank()

    /**
     * Returns the stored login name.
     */
    fun username(): String = preferences?.getString(USERNAME, "").orEmpty()

    /**
     * Returns the stored access token lifetime in seconds.
     */
    fun expiresIn(): Long = preferences?.getLong(EXPIRES_IN, 0L) ?: 0L

    /**
     * Saves a successful OAuth2 login.
     */
    fun saveLogin(username: String, accessToken: String, refreshToken: String?, expiresIn: Long) {
        preferences?.edit()
            ?.putString(USERNAME, username)
            ?.putString(ACCESS_TOKEN, accessToken)
            ?.putString(REFRESH_TOKEN, refreshToken)
            ?.putLong(EXPIRES_IN, expiresIn)
            ?.apply()
    }

    /**
     * Clears the local login state.
     */
    fun clear() {
        preferences?.edit()?.clear()?.apply()
    }
}
