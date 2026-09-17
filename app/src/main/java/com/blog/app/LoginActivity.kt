package com.blog.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
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
import com.blog.app.auth.OAuthUrlPolicy
import com.blog.app.core.config.ApiConfig
import com.blog.app.core.storage.AuthStorage
import com.blog.app.data.repository.AuthRepository
import net.openid.appauth.AuthorizationRequest

/**
 * 应用内 OAuth2 登录页面。
 */
class LoginActivity : ComponentActivity() {
    private val authRepository = AuthRepository()
    private val authUrlPolicy = OAuthUrlPolicy(ApiConfig.AUTH_BASE_URL)
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: TextView
    private lateinit var authorizationRequest: AuthorizationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        AuthStorage.initialize(applicationContext)
        buildContentView()

        authorizationRequest = savedInstanceState?.getString(KEY_AUTHORIZATION_REQUEST)
            ?.let { AuthorizationRequest.jsonDeserialize(it) }
            ?: authRepository.createAuthorizationRequest()

        webView.loadUrl(authorizationRequest.toUri().toString())
    }

    /**
     * 创建登录页面容器。
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
            CookieManager.getInstance().setAcceptCookie(true)
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
     * 创建 WebView 导航处理器，只允许访问博客授权服务。
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
     * 处理授权页面跳转，并拦截 OAuth2 回调地址。
     */
    private fun handleUrl(uri: Uri): Boolean {
        val redirectUri = Uri.parse(ApiConfig.OAUTH_REDIRECT_URI)
        if (uri.scheme.equals(redirectUri.scheme, true)
            && uri.host.equals(redirectUri.host, true)
            && uri.path == redirectUri.path
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

        if (!authUrlPolicy.isAllowed(uri.toString())) {
            return true
        }

        return false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_AUTHORIZATION_REQUEST, authorizationRequest.jsonSerializeString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val KEY_AUTHORIZATION_REQUEST = "authorization_request"
    }
}
