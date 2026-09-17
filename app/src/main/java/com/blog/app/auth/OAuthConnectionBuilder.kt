package com.blog.app.auth

import android.net.Uri
import com.blog.app.core.config.ApiConfig
import net.openid.appauth.connectivity.ConnectionBuilder
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * 为 OAuth2 token 请求提供受限的 HTTP 连接能力。
 *
 * 当前授权服务仍通过 HTTP 对外提供，因此仅允许连接到配置的授权服务来源。
 */
class OAuthConnectionBuilder(baseUrl: String) : ConnectionBuilder {
    private val allowedOrigin = URI(baseUrl).let { uri ->
        Origin(uri.scheme.lowercase(), uri.host.lowercase(), effectivePort(uri))
    }

    override fun openConnection(uri: Uri): HttpURLConnection {
        val target = URI(uri.toString())
        val scheme = target.scheme?.lowercase()
        val host = target.host?.lowercase()
        val port = effectivePort(target)

        if (scheme == "http") {
            require(
                host == allowedOrigin.host && port == allowedOrigin.port
            ) {
                "OAuth2 HTTP connection is not allowed: $uri"
            }
        } else if (scheme != "https") {
            throw IOException("Unsupported OAuth2 connection scheme: $scheme")
        }

        return if (scheme == "https") {
            DefaultConnectionBuilder.INSTANCE.openConnection(uri)
        } else {
            URL(uri.toString()).openConnection() as HttpURLConnection
        }
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
        val scheme: String,
        val host: String,
        val port: Int
    )
}
