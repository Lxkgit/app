package com.blog.app.ui.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.foundation.shape.CircleShape
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
    onCamera: () -> Unit,
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
                onFileManager = onFileManager,
                onCamera = onCamera
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
    onFileManager: () -> Unit,
    onCamera: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            UserHeader(state)
        }

        if (!state.errorMessage.isNullOrBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }

        if (state.isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        state.menus.forEach { category ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    category.menuName,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(category.children, key = { it.id }) { menu ->
                PermissionCard(
                    menu = menu,
                    onClick = if (menu.menuName == "文件云盘") onFileManager else null
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "设备监控",
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
        item {
            CameraPermissionCard(onClick = onCamera)
        }
    }
}

/**
 * 摄像头监控入口。
 */
@Composable
private fun CameraPermissionCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(108.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Text("摄像头监控", style = MaterialTheme.typography.titleMedium)
            Text("实时查看", style = MaterialTheme.typography.bodySmall)
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
        onClick = { onClick?.invoke() },
        modifier = Modifier.fillMaxWidth().height(108.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = if (menu.menuName == "文件云盘") Icons.Default.Folder else Icons.Default.Apps,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Text(menu.menuName, style = MaterialTheme.typography.titleMedium)
            Text(
                if (menu.menuName == "文件云盘") "文件管理" else "功能入口",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
