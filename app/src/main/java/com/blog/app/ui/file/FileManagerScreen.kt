package com.blog.app.ui.file

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.blog.app.data.model.file.FileDirectory
import com.blog.app.data.model.file.FileItem

/**
 * 文件云盘页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                    IconButton(onClick = {
                        if (!viewModel.goBack()) {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新建文件夹")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    errorMessage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (state.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.directories, key = { "dir-${it.id}" }) { directory ->
                    DirectoryRow(
                        directory = directory,
                        onOpen = { viewModel.enter(directory) },
                        onDelete = { viewModel.deleteDirectory(directory) }
                    )
                }
                items(state.files, key = { "file-${it.id}" }) { file ->
                    FileRow(
                        file = file,
                        onPreview = if (isPreviewable(file)) {
                            { previewFile = file }
                        } else {
                            null
                        },
                        onDelete = { viewModel.deleteFile(file) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateDirectoryDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createDirectory(name) { showCreateDialog = false }
            }
        )
    }

    previewFile?.let { file ->
        FilePreviewDialog(
            file = file,
            onDismiss = { previewFile = null }
        )
    }
}

/**
 * 目录列表项。
 */
@Composable
private fun DirectoryRow(
    directory: FileDirectory,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = "文件夹")
            Text(directory.name, modifier = Modifier.weight(1f).padding(start = 12.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除文件夹")
            }
        }
    }
}

/**
 * 文件列表项。
 */
@Composable
private fun FileRow(
    file: FileItem,
    onPreview: (() -> Unit)?,
    onDelete: () -> Unit
) {
    Card(
        onClick = { onPreview?.invoke() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.InsertDriveFile, contentDescription = "文件")
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(file.name, style = MaterialTheme.typography.bodyLarge)
                if (file.type.isNotBlank()) {
                    Text(file.type, style = MaterialTheme.typography.bodySmall)
                }
                if (onPreview != null) {
                    Text("点击查看", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除文件")
            }
        }
    }
}

/**
 * 判断文件是否支持预览。
 */
private fun isPreviewable(file: FileItem): Boolean {
    val type = file.type.lowercase()
    val name = file.name.lowercase()
    return type.startsWith("image/") || type.startsWith("video/") ||
        name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
        name.endsWith(".gif") || name.endsWith(".webp") || name.endsWith(".bmp") ||
        name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".3gp") ||
        name.endsWith(".mkv")
}

/**
 * 文件预览对话框。
 */
@Composable
private fun FilePreviewDialog(
    file: FileItem,
    onDismiss: () -> Unit
) {
    val isVideo = file.type.lowercase().startsWith("video/") ||
        file.name.lowercase().let {
            it.endsWith(".mp4") || it.endsWith(".webm") || it.endsWith(".3gp") || it.endsWith(".mkv")
        }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(file.name, style = MaterialTheme.typography.titleMedium)
                if (isVideo) {
                    VideoPreview(file.url)
                } else {
                    AsyncImage(
                        model = file.url,
                        contentDescription = file.name,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

/**
 * 视频预览播放器。
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 560.dp)
    )

    DisposableEffect(videoView) {
        onDispose {
            videoView.stopPlayback()
        }
    }
}

/**
 * 创建文件夹对话框。
 */
@Composable
private fun CreateDirectoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件夹") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("文件夹名称") }
            )
        },
        confirmButton = {
            OutlinedButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
