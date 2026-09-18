package com.blog.app.core.config

/**
 * 应用接口配置。
 */
object ApiConfig {
    /**
     * 博客内容服务基础地址。
     */
    const val BASE_URL = "http://124.221.195.130/"

    /**
     * 摄像头 WebRTC/WHEP 播放地址。
     */
    const val CAMERA_WHEP_URL = "http://124.221.195.130/rtsp/cam1/whep"

    /**
     * 对外提供给 Android 使用的 OAuth2/OIDC 授权服务基础地址。
     */
    const val AUTH_BASE_URL = "http://124.221.195.130/auth/"

    /**
     * 手机端 OAuth2 客户端标识。
     */
    const val OAUTH_CLIENT_ID = "app"

    /**
     * OAuth2 授权完成后返回 Android 应用的回调地址。
     */
    const val OAUTH_REDIRECT_URI = "com.blog.app://oauth/callback"

    /**
     * OAuth2 授权地址。
     */
    const val OAUTH_AUTHORIZATION_ENDPOINT = "${AUTH_BASE_URL}oauth2/authorize"

    /**
     * OAuth2 令牌地址。
     */
    const val OAUTH_TOKEN_ENDPOINT = "${AUTH_BASE_URL}oauth2/token"
}
