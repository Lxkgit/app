package com.blog.app.auth

import java.net.URI

/**
 * 校验应用内 OAuth2 WebView 允许访问的授权服务地址。
 */
class OAuthUrlPolicy(baseUrl: String) {
    private val origin = URI(baseUrl).let { uri ->
        Origin(uri.scheme, uri.host, effectivePort(uri))
    }

    /**
     * 判断地址是否属于配置的授权服务来源。
     */
    fun isAllowed(url: String): Boolean {
        return runCatching {
            val uri = URI(url)
            uri.scheme == origin.scheme
                && uri.host == origin.host
                && effectivePort(uri) == origin.port
        }.getOrDefault(false)
    }

    private fun effectivePort(uri: URI): Int {
        if (uri.port != -1) {
            return uri.port
        }
        return when (uri.scheme.lowercase()) {
            "https" -> 443
            "http" -> 80
            else -> -1
        }
    }

    private data class Origin(
        val scheme: String?,
        val host: String?,
        val port: Int
    )
}
