package com.blog.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.ScrollView
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
    private lateinit var debugView: TextView
    private lateinit var authorizationRequest: AuthorizationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        AuthStorage.initialize(applicationContext)
        buildContentView()

        authorizationRequest = savedInstanceState?.getString(KEY_AUTHORIZATION_REQUEST)
            ?.let { AuthorizationRequest.jsonDeserialize(it) }
            ?: authRepository.createAuthorizationRequest()

        appendDebug("授权请求已创建")
        appendDebug("client_id=${ApiConfig.OAUTH_CLIENT_ID}")
        appendDebug("authorization_uri=${authorizationRequest.toUri().getQueryParameter("redirect_uri")}")
        appendDebug("redirect_uri=${ApiConfig.OAUTH_REDIRECT_URI}")
        appendDebug("state=${authorizationRequest.state?.take(8)}...")
        appendDebug("开始加载授权页面")
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
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    appendDebug("JS ${consoleMessage.messageLevel()}: ${consoleMessage.message()}")
                    return true
                }
            }
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
                appendDebug("手动重试登录页面")
                webView.reload()
            }
        }

        debugView = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(235, 20, 20, 20))
            setPadding(16, 12, 16, 12)
            isVerticalScrollBarEnabled = true
        }

        val debugScroll = ScrollView(this).apply {
            setBackgroundColor(Color.argb(235, 20, 20, 20))
            addView(debugView, ScrollView.LayoutParams(-1, -2))
        }

        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        root.addView(progressBar, FrameLayout.LayoutParams(96, 96).apply {
            gravity = android.view.Gravity.CENTER
        })
        root.addView(errorView, FrameLayout.LayoutParams(-1, -1))
        root.addView(debugScroll, FrameLayout.LayoutParams(-1, 300).apply {
            gravity = android.view.Gravity.BOTTOM
        })
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
                appendDebug("页面开始加载: ${url ?: "null"}")
                url?.let { handleUrl(Uri.parse(it)) }
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                appendDebug("页面加载完成: ${url ?: "null"}")
                super.onPageFinished(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                appendDebug("拦截导航: ${request.url}")
                return handleUrl(request.url)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                appendDebug("拦截导航: $url")
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
                    appendDebug("页面错误: code=${error.errorCode}, description=${error.description}, url=${request.url}")
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
            appendDebug("检测到 OAuth2 回调")
            appendDebug("callback scheme=${uri.scheme}, host=${uri.host}, path=${uri.path}")
            appendDebug("callback code=${if (uri.getQueryParameter("code").isNullOrBlank()) "缺失" else "已返回"}")
            appendDebug("callback state=${if (uri.getQueryParameter("state").isNullOrBlank()) "缺失" else "已返回"}")
            appendDebug("callback error=${uri.getQueryParameter("error") ?: "无"}")
            progressBar.visibility = View.VISIBLE
            webView.visibility = View.INVISIBLE
            authRepository.exchangeAuthorizationCode(this, authorizationRequest, uri) { result ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    result.onSuccess {
                        appendDebug("OAuth2 token 交换成功，登录完成")
                        setResult(RESULT_OK)
                        finish()
                    }.onFailure { error ->
                        appendDebug("OAuth2 token 交换失败: ${error.javaClass.simpleName}: ${error.message}")
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
            appendDebug("阻止非授权服务地址: $uri")
            return true
        }

        return false
    }

    /**
     * 向页面底部调试窗口追加诊断信息。
     */
    private fun appendDebug(message: String) {
        if (!::debugView.isInitialized) {
            return
        }
        runOnUiThread {
            val current = debugView.text?.toString().orEmpty()
            val lines = (current + "\n" + message).lines().takeLast(80)
            debugView.text = lines.joinToString("\n")
            debugView.post {
                (debugView.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN)
            }
        }
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
