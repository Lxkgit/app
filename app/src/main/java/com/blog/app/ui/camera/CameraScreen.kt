package com.blog.app.ui.camera

import android.app.Activity
import android.content.pm.ActivityInfo
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.webrtc.SurfaceViewRenderer

/**
 * 摄像头监控页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBack: () -> Unit, viewModel: CameraViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var renderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var player by remember { mutableStateOf<WebRtcCameraPlayer?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var fullScreen by remember { mutableStateOf(false) }
    val activity = androidx.compose.ui.platform.LocalContext.current as? Activity

    LaunchedEffect(fullScreen) {
        activity ?: return@LaunchedEffect
        activity.requestedOrientation = if (fullScreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        if (fullScreen) {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
        }
    }
    val activity = (LocalContext.current as? Activity)

    fun enterFullScreen() {
        fullScreen = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.window?.decorView?.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    fun exitFullScreen() {
        fullScreen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.window?.decorView?.systemUiVisibility = 0
    }

    BackHandler {
        if (fullScreen) {
            exitFullScreen()
        } else {
            viewModel.stop(player)
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (fullScreen) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                activity?.window?.decorView?.systemUiVisibility = 0
            }
            viewModel.stop(player)
            player?.release()
            player = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.let {
                WindowInsetsControllerCompat(it.window, it.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(it.window, true)
            }
            renderer = null
        }
    }

    LaunchedEffect(renderer, retryKey) {
        if (renderer == null) return@LaunchedEffect
        val currentPlayer = player ?: return@LaunchedEffect
        viewModel.play(currentPlayer)
    }

    Scaffold(
        containerColor = if (fullScreen) Color.Black else MaterialTheme.colorScheme.background,
        topBar = {
            if (!fullScreen) {
                TopAppBar(title = { Text("摄像头") }, navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stop(player)
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                })
            }
        }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(if (fullScreen) androidx.compose.foundation.layout.PaddingValues(0.dp) else padding)
        ) {
            Box(
                modifier = if (fullScreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                }.background(Color.Black), contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(), factory = { context ->
                        SurfaceViewRenderer(context).also {
                            renderer = it
                            player = WebRtcCameraPlayer(context, it)
                        }
                    })

                if (state.isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            "正在连接摄像头...", Modifier.padding(top = 10.dp), color = Color.White
                        )
                    }
                }

                if (!state.errorMessage.isNullOrBlank()) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(state.errorMessage.orEmpty(), color = Color.White)
                        Button(onClick = {
                            retryKey++
                            viewModel.clearError()
                        }) {
                            Text("重新连接")
                        }
                    }
                }

                if (!fullScreen) {
                    IconButton(
                        onClick = { enterFullScreen() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "全屏",
                            tint = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = { fullScreen = !fullScreen },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Icon(
                        if (fullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (fullScreen) "退出全屏" else "全屏",
                        tint = Color.White
                    )
                }

                if (fullScreen) {
                    Text(
                        "● LIVE  ·  CAM 01",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 16.dp, top = 28.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                    IconButton(
                        onClick = { exitFullScreen() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 18.dp, end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.FullscreenExit,
                            contentDescription = "退出全屏",
                            tint = Color.White
                        )
                    }
                }
            }

            if (!fullScreen) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("摄像头 01", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (state.playing) "● 实时监控 · 已连接" else "正在建立连接",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
