package com.blog.app.ui.camera

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blog.app.R
import androidx.compose.ui.res.painterResource
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

    BackHandler(onBack = onBack)

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
            player = null
            renderer = null
        }
    }

    LaunchedEffect(renderer, retryKey) {
        if (renderer == null) return@LaunchedEffect
        val currentPlayer = player ?: return@LaunchedEffect
        viewModel.play(currentPlayer)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("摄像头监控") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.icon_back),
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        SurfaceViewRenderer(context).also { surfaceViewRenderer ->
                            renderer = surfaceViewRenderer
                            player = WebRtcCameraPlayer(context, surfaceViewRenderer)
                        }
                    }
                )

                if (state.isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            "正在连接摄像头...",
                            modifier = Modifier.padding(top = 12.dp),
                            color = Color.White
                        )
                    }
                }

                if (!state.errorMessage.isNullOrBlank()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            state.errorMessage.orEmpty(),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = {
                            retryKey++
                            viewModel.clearError()
                        }) {
                            Text("重新连接")
                        }
                    }
                }
            }
        }
    }
}
