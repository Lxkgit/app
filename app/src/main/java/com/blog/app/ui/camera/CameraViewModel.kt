package com.blog.app.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blog.app.core.config.ApiConfig
import com.blog.app.data.repository.CameraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 摄像头监控页面状态。
 */
class CameraViewModel(
    private val repository: CameraRepository = CameraRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    /**
     * 获取摄像头 Token 并建立 WebRTC 播放连接。
     */
    fun play(player: WebRtcCameraPlayer) {
        viewModelScope.launch {
            _uiState.value = CameraUiState(isLoading = true)
            runCatching {
                val cameraToken = repository.createToken(CAMERA_STREAM)
                player.play(ApiConfig.CAMERA_WHEP_URL, cameraToken.token)
            }.onSuccess {
                _uiState.value = CameraUiState(isLoading = false, playing = true)
            }.onFailure { throwable ->
                _uiState.value = CameraUiState(
                    isLoading = false,
                    errorMessage = throwable.message ?: "摄像头连接失败"
                )
            }
        }
    }

    /**
     * 清除页面错误状态。
     */
    fun clearError() {
        _uiState.value = CameraUiState()
    }

    companion object {
        private const val CAMERA_STREAM = "cam1"
    }
}

/**
 * 摄像头页面状态。
 */
data class CameraUiState(
    val isLoading: Boolean = false,
    val playing: Boolean = false,
    val errorMessage: String? = null
)
