package com.blog.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.blog.app.core.storage.AuthStorage
import com.blog.app.data.repository.AuthRepository
import com.blog.app.navigation.AppNavigation

/**
 * Main Android activity.
 */
class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.data != null) {
            handleAuthIntent(result.data)
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
                            authLauncher.launch(authRepository.authorizationIntent(this))
                        }
                    )
                }
            }
        }
    }

    /**
     * Handles the OAuth2 redirect returned by the authorization server.
     */
    private fun handleAuthIntent(intent: Intent?) {
        if (intent == null) {
            return
        }

        authRepository.handleAuthorizationResponse(this, intent) { result ->
            runOnUiThread {
                result.onFailure { error ->
                    Toast.makeText(
                        this,
                        error.message ?: "登录失败",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
