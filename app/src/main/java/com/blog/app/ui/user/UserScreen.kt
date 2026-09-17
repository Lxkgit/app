package com.blog.app.ui.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.blog.app.data.model.menu.UserMenu

/**
 * 个人页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    onLogin: () -> Unit,
    onSettings: () -> Unit,
    onFileManager: () -> Unit,
    refreshKey: Int = 0,
    viewModel: UserViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) {
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loggedIn) {
            UserInfoView(
                modifier = Modifier.fillMaxSize().padding(padding),
                state = state,
                onFileManager = onFileManager
            )
        } else {
            LoginView(
                modifier = Modifier.fillMaxSize().padding(padding),
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
 * 显示博客登录入口。
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
        Text("登录博客", style = MaterialTheme.typography.headlineMedium)
        Text("点击登录后将在应用内完成博客账号认证。", style = MaterialTheme.typography.bodyMedium)
        if (!errorMessage.isNullOrBlank()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("登录博客")
        }
    }
}

/**
 * 显示用户资料和权限菜单。
 */
@Composable
private fun UserInfoView(
    modifier: Modifier,
    state: UserUiState,
    onFileManager: () -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { UserHeader(state) }
        if (!state.errorMessage.isNullOrBlank()) {
            item { Text(state.errorMessage, color = MaterialTheme.colorScheme.error) }
        }
        if (state.isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
        state.menus.forEach { category ->
            item {
                Text(
                    category.menuName,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(category.children, key = { it.id }) { menu ->
                PermissionCard(menu = menu, onClick = if (menu.menuName == "文件云盘") onFileManager else null)
            }
        }
    }
}

/**
 * 用户头像和登录信息。
 */
@Composable
private fun UserHeader(state: UserUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.avatar.isNotBlank()) {
                AsyncImage(
                    model = state.avatar,
                    contentDescription = "用户头像",
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Card(modifier = Modifier.size(64.dp), shape = CircleShape) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state.username.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(state.username, style = MaterialTheme.typography.titleLarge)
                Text("已登录", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * 单个权限菜单卡片。
 */
@Composable
private fun PermissionCard(menu: UserMenu, onClick: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() }
    ) {
        Text(
            menu.menuName,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
