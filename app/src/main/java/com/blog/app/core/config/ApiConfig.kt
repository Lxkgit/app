package com.blog.app.core.config

/**
 * Application API configuration.
 */
object ApiConfig {
    /**
     * Blog content service base URL.
     */
    const val BASE_URL = "http://124.221.195.130/api/content/"

    /**
     * Public OAuth2/OIDC authorization service base URL.
     */
    const val AUTH_BASE_URL = "http://124.221.195.130/auth/"

    /**
     * Android public OAuth2 client identifier.
     */
    const val OAUTH_CLIENT_ID = "blog-android"

    /**
     * OAuth2 callback URI registered for the Android application.
     */
    const val OAUTH_REDIRECT_URI = "com.blog.app://oauth/callback"

    /**
     * OAuth2 authorization endpoint.
     */
    const val OAUTH_AUTHORIZATION_ENDPOINT = "${AUTH_BASE_URL}oauth2/authorize"

    /**
     * OAuth2 token endpoint.
     */
    const val OAUTH_TOKEN_ENDPOINT = "${AUTH_BASE_URL}oauth2/token"
}
