package com.blog.app.ui.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Personal page with the blog OAuth2 authentication flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    onLogin: () -> Unit,
    viewModel: UserViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("我的") })
        }
    ) { padding ->
        if (state.loggedIn) {
            UserInfoView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                username = state.username,
                onLogout = viewModel::logout
            )
        } else {
            LoginView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                onLogin = {
                    viewModel.beginLogin()
                    onLogin()
                }
            )
        }
    }
}

/**
 * Displays the blog login entry point.
 */
@Composable
private fun LoginView(
    modifier: Modifier,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "登录博客",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "点击登录后将在浏览器中完成博客账号认证。",
            style = MaterialTheme.typography.bodyMedium
        )
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text("登录博客")
            }
        }
    }
}

/**
 * Displays the authenticated user area.
 */
@Composable
private fun UserInfoView(
    modifier: Modifier,
    username: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("已登录", style = MaterialTheme.typography.titleLarge)
                Text(
                    username,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "博客 OAuth2 登录已接入。",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("退出登录")
        }
    }
}
