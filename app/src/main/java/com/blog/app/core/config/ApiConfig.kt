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
     * OAuth2 client identifier registered for the blog application.
     */
    const val OAUTH_CLIENT_ID = "blog"

    /**
     * Client secret required by the existing password grant endpoint.
     */
    const val OAUTH_CLIENT_SECRET = "123456"
}
