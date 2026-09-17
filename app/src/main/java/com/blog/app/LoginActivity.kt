package com.blog.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import com.blog.app.core.config.ApiConfig
import com.blog.app.core.storage.AuthStorage
import com.blog.app.data.repository.AuthRepository

/**
 * 应用内 OAuth2 登录页面。
 */
class LoginActivity : ComponentActivity() {
    private val authRepository = AuthRepository()
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: TextView
    private lateinit var authorizationRequest: net.openid.appauth.AuthorizationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        AuthStorage.initialize(applicationContext)
        buildContentView()

        authorizationRequest = authRepository.createAuthorizationRequest()
        webView.loadUrl(authorizationRequest.toUri().toString())
    }

    /**
     * 创建与博客登录页视觉连续的 WebView 容器。
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun buildContentView() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
        }

        webView = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = createWebViewClient()
        }

        progressBar = ProgressBar(this).apply {
            isIndeterminate = true
        }

        errorView = TextView(this).apply {
            text = "登录页面加载失败，点击重试"
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
            setOnClickListener {
                visibility = View.GONE
                webView.visibility = View.VISIBLE
                webView.reload()
            }
        }

        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        root.addView(progressBar, FrameLayout.LayoutParams(96, 96).apply {
            gravity = android.view.Gravity.CENTER
        })
        root.addView(errorView, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    /**
     * 创建 WebView 导航处理器，只允许登录页面继续访问授权服务。
     */
    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
                errorView.visibility = View.GONE
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                super.onPageFinished(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return handleUrl(request.url)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUrl(Uri.parse(url))
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    progressBar.visibility = View.GONE
                    errorView.visibility = View.VISIBLE
                }
                super.onReceivedError(view, request, error)
            }
        }
    }

    /**
     * 拦截 OAuth2 回调，并在应用内完成授权码换令牌。
     */
    private fun handleUrl(uri: Uri): Boolean {
        if (uri.scheme.equals(Uri.parse(ApiConfig.OAUTH_REDIRECT_URI).scheme, true)
            && uri.host == Uri.parse(ApiConfig.OAUTH_REDIRECT_URI).host
            && uri.path == Uri.parse(ApiConfig.OAUTH_REDIRECT_URI).path
        ) {
            progressBar.visibility = View.VISIBLE
            webView.visibility = View.INVISIBLE
            authRepository.exchangeAuthorizationCode(this, authorizationRequest, uri) { result ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    result.onSuccess {
                        setResult(RESULT_OK)
                        finish()
                    }.onFailure { error ->
                        webView.visibility = View.VISIBLE
                        Toast.makeText(
                            this,
                            error.message ?: "登录失败",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            return true
        }

        val targetUri = Uri.parse(uri.toString())
        val authUri = Uri.parse(ApiConfig.AUTH_BASE_URL)
        if (targetUri.scheme != authUri.scheme || targetUri.host != authUri.host) {
            return true
        }

        return false
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
