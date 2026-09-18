package com.blog.app.core.auth

import android.os.Handler
import android.os.Looper
import com.blog.app.core.storage.AuthStorage
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 管理登录状态失效后的自动重新登录流程。
 */
object AuthSessionManager {
    private const val AUTH_RETRY_HEADER = "X-Blog-Auth-Retry"
    private const val REFRESH_RETRY_HEADER = "X-Blog-Auth-Refresh-Retry"

    private val lock = ReentrantLock()
    private val loginCondition = lock.newCondition()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var loginLauncher: (() -> Unit)? = null
    private var waitingForLogin = false
    private var loginResult: Boolean? = null

    /**
     * 注册登录页面启动回调。
     */
    fun registerLoginLauncher(launcher: () -> Unit) {
        lock.withLock {
            loginLauncher = launcher
        }
    }

    /**
     * 解除登录页面启动回调。
     */
    fun unregisterLoginLauncher() {
        lock.withLock {
            loginLauncher = null
        }
    }

    /**
     * 通知登录页面已经完成登录或取消登录。
     */
    fun onLoginResult(success: Boolean) {
        lock.withLock {
            if (!waitingForLogin) {
                return
            }
            loginResult = success
            waitingForLogin = false
            loginCondition.signalAll()
        }
    }

    /**
     * 清除失效登录状态，并等待用户重新登录。
     *
     * 返回 true 表示已经重新登录成功，可以重试原请求。
     */
    fun waitForReLogin(): Boolean {
        AuthStorage.clear()

        var shouldLaunchLogin = false
        var launcher: (() -> Unit)? = null

        lock.withLock {
            if (!waitingForLogin) {
                waitingForLogin = true
                loginResult = null
                shouldLaunchLogin = true
                launcher = loginLauncher
            }
        }

        if (shouldLaunchLogin) {
            val loginLauncher = launcher
            if (loginLauncher == null) {
                lock.withLock {
                    waitingForLogin = false
                    loginResult = false
                    loginCondition.signalAll()
                }
                return false
            }
            mainHandler.post {
                loginLauncher.invoke()
            }
        }

        lock.withLock {
            while (waitingForLogin) {
                try {
                    loginCondition.await()
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
     * 判断请求是否已经使用过 refresh_token 重试。
     */
    fun isRefreshRetryRequest(request: okhttp3.Request): Boolean =
        request.header(REFRESH_RETRY_HEADER) == "1"

    /**
     * 为 refresh_token 重试添加一次性标记。
     */
    fun markRefreshRetry(request: okhttp3.Request): okhttp3.Request =
        request.newBuilder()
            .header(REFRESH_RETRY_HEADER, "1")
            .build()

    /**
     * 为重新登录后的请求添加一次性重试标记。
     */
    fun markRetry(request: okhttp3.Request): okhttp3.Request =
        request.newBuilder()
            .header(AUTH_RETRY_HEADER, "1")
            .build()
}
