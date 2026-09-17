package com.blog.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.blog.app.core.storage.AuthStorage
import com.blog.app.navigation.AppNavigation

/**
 * 应用主 Activity。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthStorage.initialize(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        onLogin = {
                            startActivityForResult(
                                Intent(this, LoginActivity::class.java),
                                LOGIN_REQUEST_CODE
                            )
                        }
                    )
                }
            }
        }
    }

    /**
     * 接收登录页面结果并刷新当前页面。
     */
    @Deprecated("使用 Activity Result API 时无需手动处理登录页面结果")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE && resultCode == RESULT_OK) {
            recreate()
        }
    }

    companion object {
        private const val LOGIN_REQUEST_CODE = 1001
    }
}
