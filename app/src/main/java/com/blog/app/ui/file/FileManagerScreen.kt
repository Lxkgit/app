package com.blog.app.ui.file

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.blog.app.data.model.file.FileDirectory
import com.blog.app.data.model.file.FileItem

/**
 * 文件云盘页面。
 */
@Composable
fun FileManagerScreen(
    onBack: () -> Unit,
    viewModel: FileManagerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage
    var showCreateDialog by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<FileItem?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.path.isNullOrBlank()) "文件云盘" else state.path ?: "文件云盘") },
                navigationIcon = {
                    IconButton(onClick = { if (!viewModel.goBack()) onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新建文件夹")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!errorMessage.isNullOrBlank()) {
                Text(errorMessage, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.error)
            }
            if (state.isLoading) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = rememberLazyGridState(),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.directories, key = { "dir-" + it.id }) { directory ->
                    DirectoryCard(directory, { viewModel.enter(directory) }, { viewModel.deleteDirectory(directory) })
                }
                items(state.files, key = { "file-" + it.id }) { file ->
                    FileCard(file, { previewFile = file }, { viewModel.deleteFile(file) })
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateDirectoryDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name -> viewModel.createDirectory(name) { showCreateDialog = false } }
        )
    }

    previewFile?.let { file ->
        FilePreviewDialog(file = file, onDismiss = { previewFile = null })
    }
}

/**
 * 文件夹卡片。
 */
@Composable
private fun DirectoryCard(directory: FileDirectory, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().height(118.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            Icon(Icons.Default.Folder, null, Modifier.align(Alignment.TopStart), tint = MaterialTheme.colorScheme.primary)
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(directory.name, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                Text("文件夹", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * 文件卡片，图片和视频直接显示缩略图。
 */
@Composable
private fun FileCard(file: FileItem, onPreview: () -> Unit, onDelete: () -> Unit) {
    val previewable = isPreviewable(file)
    val image = isImage(file)

    Card(
        onClick = if (previewable) onPreview else ({}),
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            if (image) {
                AsyncImage(
                    model = file.url,
                    contentDescription = file.name,
                    modifier = Modifier.fillMaxWidth().height(118.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().height(118.dp).background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isVideo(file)) Icons.Default.PlayCircle else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.height(42.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(file.name, maxLines = 1, style = MaterialTheme.typography.labelLarge)
                    Text(file.type.ifBlank { "文件" }, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

/**
 * 判断文件是否为图片。
 */
private fun isImage(file: FileItem): Boolean {
    val type = file.type.lowercase()
    val name = file.name.lowercase()
    return type.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
        name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".webp") || name.endsWith(".bmp")
}

/**
 * 判断文件是否为视频。
 */
private fun isVideo(file: FileItem): Boolean {
    val type = file.type.lowercase()
    val name = file.name.lowercase()
    return type.startsWith("video/") || name.endsWith(".mp4") || name.endsWith(".webm") ||
        name.endsWith(".3gp") || name.endsWith(".mkv")
}

/**
 * 判断文件是否支持预览。
 */
private fun isPreviewable(file: FileItem): Boolean = isImage(file) || isVideo(file)

/**
 * 全屏媒体预览。
 */
@Composable
private fun FilePreviewDialog(file: FileItem, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (isVideo(file)) {
                VideoPreview(file.url)
            } else {
                AsyncImage(
                    model = file.url,
                    contentDescription = file.name,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Row(
                Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(horizontal = 8.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(file.name, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
                }
            }
        }
    }
}

/**
 * 全屏视频预览播放器。
 */
@Composable
private fun VideoPreview(url: String) {
    val context = LocalContext.current
    val videoView = remember(url) {
        VideoView(context).apply {
            setMediaController(MediaController(context))
            setVideoURI(Uri.parse(url))
            start()
        }
    }
    AndroidView(
        factory = { videoView },
        modifier = Modifier.fillMaxSize()
    )
    DisposableEffect(videoView) {
        onDispose { videoView.stopPlayback() }
    }
}

/**
 * 创建文件夹对话框。
 */
@Composable
private fun CreateDirectoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件夹") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("文件夹名称") })
        },
        confirmButton = {
            OutlinedButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("创建") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
