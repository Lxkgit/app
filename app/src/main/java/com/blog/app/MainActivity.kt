package com.blog.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthStorage.initialize(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        onLogin = {
                            loginLauncher.launch(Intent(this, LoginActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}
