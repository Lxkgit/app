package com.blog.app.ui.camera

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

/**
 * 摄像头监控页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    viewModel: CameraViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var renderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var player by remember { mutableStateOf<WebRtcCameraPlayer?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var fullScreen by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    /**
     * 切换横竖屏和系统栏显示状态。
     */
    fun updateFullScreenWindow(enabled: Boolean) {
        activity ?: return

        activity.requestedOrientation = if (enabled) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val controller = WindowInsetsControllerCompat(
            activity.window,
            activity.window.decorView
        )

        if (enabled) {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            renderer?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
            renderer?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        }
    }

    LaunchedEffect(fullScreen) {
        updateFullScreenWindow(fullScreen)
    }

    BackHandler {
        if (fullScreen) {
            fullScreen = false
        } else {
            viewModel.stop(player)
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stop(player)
            player?.release()
            player = null
            renderer = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.let {
                val controller = WindowInsetsControllerCompat(
                    it.window,
                    it.window.decorView
                )
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(it.window, true)
            }
        }
    }

    LaunchedEffect(renderer, retryKey) {
        renderer?.setScalingType(
            if (fullScreen) {
                RendererCommon.ScalingType.SCALE_ASPECT_FILL
            } else {
                RendererCommon.ScalingType.SCALE_ASPECT_FIT
            }
        )

        val currentRenderer = renderer ?: return@LaunchedEffect
        val currentPlayer = player ?: return@LaunchedEffect
        viewModel.play(currentPlayer)
    }

    Scaffold(
        containerColor = if (fullScreen) {
            Color.Black
        } else {
            MaterialTheme.colorScheme.background
        },
        topBar = {
            if (!fullScreen) {
                TopAppBar(
                    title = { Text("摄像头") },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.stop(player)
                            onBack()
                        }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    if (fullScreen) {
                        PaddingValues(0.dp)
                    } else {
                        padding
                    }
                )
        ) {
            Box(
                modifier = if (fullScreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                }.background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        SurfaceViewRenderer(context).also {
                            renderer = it
                            player = WebRtcCameraPlayer(context, it)
                        }
                    }
                )

                if (state.isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "正在连接摄像头...",
                            Modifier.padding(top = 10.dp),
                            color = Color.White
                        )
                    }
                }

                if (!state.errorMessage.isNullOrBlank()) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            state.errorMessage.orEmpty(),
                            color = Color.White
                        )
                        Button(onClick = {
                            retryKey++
                            viewModel.clearError()
                        }) {
                            Text("重新连接")
                        }
                    }
                }

                IconButton(
                    onClick = { fullScreen = !fullScreen },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Icon(
                        if (fullScreen) {
                            Icons.Default.FullscreenExit
                        } else {
                            Icons.Default.Fullscreen
                        },
                        contentDescription = if (fullScreen) {
                            "退出全屏"
                        } else {
                            "全屏"
                        },
                        tint = Color.White
                    )
                }

                if (fullScreen) {
                    Text(
                        "● LIVE  ·  CAM 01",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 16.dp, top = 16.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (!fullScreen) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "摄像头 01",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        if (state.playing) {
                            "● 实时监控 · 已连接"
                        } else {
                            "正在建立连接"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.playing) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
