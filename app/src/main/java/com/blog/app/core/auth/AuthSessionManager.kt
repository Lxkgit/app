package com.blog.app.core.auth

import android.os.Handler
import android.os.Looper
import com.blog.app.core.storage.AuthStorage

/**
 * 管理登录状态失效后的自动重新登录流程。
 */
object AuthSessionManager {
    private const val AUTH_RETRY_HEADER = "X-Blog-Auth-Retry"

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var loginLauncher: (() -> Unit)? = null
    private var waitingForLogin = false
    private var loginResult: Boolean? = null

    /**
     * 注册登录页面启动回调。
     */
    fun registerLoginLauncher(launcher: () -> Unit) {
        synchronized(lock) {
            loginLauncher = launcher
        }
    }

    /**
     * 解除登录页面启动回调。
     */
    fun unregisterLoginLauncher() {
        synchronized(lock) {
            loginLauncher = null
        }
    }

    /**
     * 通知登录页面已经完成登录或取消登录。
     */
    fun onLoginResult(success: Boolean) {
        synchronized(lock) {
            if (!waitingForLogin) {
                return
            }
            loginResult = success
            waitingForLogin = false
            lock.notifyAll()
        }
    }

    /**
     * 清除失效登录状态，并等待用户重新登录。
     *
     * 返回 true 表示已经重新登录成功，可以重试原请求。
     */
    fun waitForReLogin(): Boolean {
        AuthStorage.clear()

        val shouldLaunchLogin: Boolean
        val launcher: (() -> Unit)?
        synchronized(lock) {
            shouldLaunchLogin = !waitingForLogin
            if (shouldLaunchLogin) {
                waitingForLogin = true
                loginResult = null
            }
            launcher = loginLauncher
        }

        if (shouldLaunchLogin && launcher != null) {
            mainHandler.post {
                launcher.invoke()
            }
        }

        synchronized(lock) {
            while (waitingForLogin) {
                try {
                    lock.wait()
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return false
                }
            }
            return loginResult == true
        }
    }

    /**
     * 判断请求是否已经进行过一次认证失效后的自动重试。
     */
    fun isRetryRequest(request: okhttp3.Request): Boolean =
        request.header(AUTH_RETRY_HEADER) == "1"

    /**
     * 为重新登录后的请求添加一次性重试标记。
     */
    fun markRetry(request: okhttp3.Request): okhttp3.Request =
        request.newBuilder()
            .header(AUTH_RETRY_HEADER, "1")
            .build()
}
