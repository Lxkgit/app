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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(refreshKey) { if (refreshKey > 0) viewModel.refresh() }

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
            UserInfoView(Modifier.fillMaxSize().padding(padding), state, onFileManager, onCamera)
        } else {
            LoginView(Modifier.fillMaxSize().padding(padding), state.isLoading, state.errorMessage, onLogin)
        }
    }
}

/**
 * 登录入口。
 */
@Composable
private fun LoginView(modifier: Modifier, isLoading: Boolean, errorMessage: String?, onLogin: () -> Unit) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(76.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            BoxCenter { Text("B", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) }
        }
        Text("登录博客", style = MaterialTheme.typography.headlineSmall)
        Text("登录后访问你的文章、文件和设备", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!errorMessage.isNullOrBlank()) Text(errorMessage, color = MaterialTheme.colorScheme.error)
        Button(onClick = onLogin, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("登录")
        }
    }
}

/**
 * 用户信息和功能入口。
 */
@Composable
private fun UserInfoView(modifier: Modifier, state: UserUiState, onFileManager: () -> Unit, onCamera: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { UserHeader(state) }

        if (!state.errorMessage.isNullOrBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }

        state.menus.forEach { category ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(category.menuName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
            }
            items(category.children, key = { it.id }) { menu ->
                PermissionCard(menu, if (menu.menuName == "文件云盘") onFileManager else null)
            }
            if (category.menuName == "设备管理") item {
                CameraPermissionCard(onCamera)
            }
        }
    }
}

/**
 * 摄像头入口。
 */
@Composable
private fun CameraPermissionCard(onClick: () -> Unit) {
    SmallFunctionCard(
        title = "摄像头",
        subtitle = "实时查看",
        icon = { Icon(Icons.Default.Videocam, null, Modifier.size(24.dp)) },
        onClick = onClick
    )
}

/**
 * 用户头像区域。
 */
@Composable
private fun UserHeader(state: UserUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.avatar.isNotBlank()) {
                AsyncImage(
                    model = state.avatar,
                    contentDescription = "用户头像",
                    modifier = Modifier.size(58.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Card(modifier = Modifier.size(58.dp), shape = CircleShape) {
                    BoxCenter { Text(state.username.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall) }
                }
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(state.username, style = MaterialTheme.typography.titleLarge)
                Text("账号已登录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
            }
        }
    }
}

/**
 * 权限入口卡片。
 */
@Composable
private fun PermissionCard(menu: UserMenu, onClick: (() -> Unit)?) {
    SmallFunctionCard(
        title = menu.menuName,
        subtitle = if (menu.menuName == "文件云盘") "文件管理" else "功能入口",
        icon = {
            Icon(
                if (menu.menuName == "文件云盘") Icons.Default.Folder else Icons.Default.Apps,
                null,
                Modifier.size(24.dp)
            )
        },
        onClick = onClick
    )
}

/**
 * 紧凑功能卡片。
 */
@Composable
private fun SmallFunctionCard(title: String, subtitle: String, icon: @Composable () -> Unit, onClick: (() -> Unit)?) {
    Card(
        onClick = { onClick?.invoke() },
        modifier = Modifier.fillMaxWidth().height(94.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxSize().padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
            icon()
            Column {
                Text(title, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * 居中容器。
 */
@Composable
private fun BoxCenter(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}
