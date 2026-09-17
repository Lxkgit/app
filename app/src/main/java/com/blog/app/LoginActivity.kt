package com.blog.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import com.blog.app.debug.DebugLog
import com.blog.app.debug.DebugOverlay
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

        DebugLog.add("OAuth2 授权请求已创建")
        DebugLog.add("授权地址: ${ApiConfig.OAUTH_AUTHORIZATION_ENDPOINT}")
        DebugLog.add("客户端: ${ApiConfig.OAUTH_CLIENT_ID}")
        DebugLog.add("回调地址: ${ApiConfig.OAUTH_REDIRECT_URI}")
        DebugLog.add("开始加载授权页面: ${authorizationRequest.toUri()}")
        Log.d(TAG, "授权请求已创建")
        Log.d(TAG, "authorizationEndpoint=${ApiConfig.OAUTH_AUTHORIZATION_ENDPOINT}")
        Log.d(TAG, "clientId=${ApiConfig.OAUTH_CLIENT_ID}")
        Log.d(TAG, "redirectUri=${ApiConfig.OAUTH_REDIRECT_URI}")
        Log.d(TAG, "state=${authorizationRequest.state?.take(8)}...")
        Log.d(TAG, "开始加载授权页面: ${authorizationRequest.toUri()}")
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
                DebugLog.add("手动重试登录页面")
                Log.d(TAG, "手动重试登录页面")
                webView.reload()
            }
        }

        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        root.addView(progressBar, FrameLayout.LayoutParams(96, 96).apply {
            gravity = android.view.Gravity.CENTER
        })
        root.addView(errorView, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        DebugOverlay.attach(this, root)
    }

    /**
     * 创建 WebView 导航处理器，只允许访问博客授权服务。
     */
    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
                errorView.visibility = View.GONE
                DebugLog.add("页面开始加载: ${url ?: "null"}")
                Log.d(TAG, "页面开始加载: ${url ?: "null"}")
                if (url != null && handleUrl(Uri.parse(url))) {
                    view?.stopLoading()
                }
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                DebugLog.add("页面加载完成: ${url ?: "null"}")
                Log.d(TAG, "页面加载完成: ${url ?: "null"}")
                super.onPageFinished(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                DebugLog.add("WebView 导航 ${request.method}: ${request.url}")
                Log.d(TAG, "WebView 导航: ${request.url}")
                return handleUrl(request.url)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                DebugLog.add("WebView 导航: $url")
                Log.d(TAG, "WebView 导航: $url")
                return handleUrl(Uri.parse(url))
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                DebugLog.add("WebView 请求 ${request.method}: ${request.url}")
                return super.shouldInterceptRequest(view, request)
            }

            @Suppress("DEPRECATION")
            override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                DebugLog.add("WebView 请求: $url")
                return super.shouldInterceptRequest(view, url)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    progressBar.visibility = View.GONE
                    errorView.visibility = View.VISIBLE
                    DebugLog.add("页面加载失败 code=${error.errorCode}, description=${error.description}, url=${request.url}")
                    Log.e(TAG, "页面加载失败: code=${error.errorCode}, description=${error.description}, url=${request.url}")
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
            DebugLog.add("检测到 OAuth2 回调: ${uri.scheme}://${uri.host}${uri.path}")
            DebugLog.add("callback code=${if (uri.getQueryParameter("code").isNullOrBlank()) "缺失" else "已返回"}")
            DebugLog.add("callback state=${if (uri.getQueryParameter("state").isNullOrBlank()) "缺失" else "已返回"}")
            DebugLog.add("callback error=${uri.getQueryParameter("error") ?: "无"}")
            Log.d(TAG, "检测到 OAuth2 回调: scheme=${uri.scheme}, host=${uri.host}, path=${uri.path}")
            Log.d(TAG, "callback code=${if (uri.getQueryParameter("code").isNullOrBlank()) "缺失" else "已返回"}")
            Log.d(TAG, "callback state=${if (uri.getQueryParameter("state").isNullOrBlank()) "缺失" else "已返回"}")
            Log.d(TAG, "callback error=${uri.getQueryParameter("error") ?: "无"}")
            progressBar.visibility = View.VISIBLE
            webView.visibility = View.INVISIBLE
            authRepository.exchangeAuthorizationCode(this, authorizationRequest, uri) { result ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    result.onSuccess {
                        DebugLog.add("OAuth2 token 交换成功，登录完成")
                        Log.d(TAG, "OAuth2 token 交换成功，登录完成")
                        setResult(RESULT_OK)
                        finish()
                    }.onFailure { error ->
                        DebugLog.add("OAuth2 token 交换失败: ${error.javaClass.simpleName}: ${error.message}")
                        Log.e(TAG, "OAuth2 token 交换失败: ${error.javaClass.simpleName}: ${error.message}", error)
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
            DebugLog.add("阻止非授权服务地址: $uri")
            Log.w(TAG, "阻止非授权服务地址: $uri")
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
        private const val TAG = "BLOG_OAUTH"
        private const val KEY_AUTHORIZATION_REQUEST = "authorization_request"
    }
}
