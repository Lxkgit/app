package com.blog.app.ui.user

import androidx.lifecycle.ViewModel
import com.blog.app.core.storage.AuthStorage
import com.blog.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 个人页面的认证界面状态。
 */
data class UserUiState(
    val loggedIn: Boolean = AuthStorage.isLoggedIn(),
    val username: String = AuthStorage.username(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 协调个人页面的 OAuth2 登录和退出流程。
 */
class UserViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    /**
     * 从本地持久化存储刷新当前登录状态。
     */
    fun refresh() {
        _uiState.value = UserUiState(
            loggedIn = AuthStorage.isLoggedIn(),
            username = AuthStorage.username(),
            isLoading = false,
            errorMessage = _uiState.value.errorMessage
        )
    }

    /**
     * 将授权流程标记为加载中。
     */
    fun beginLogin() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
    }

    /**
     * 保存授权失败信息供界面显示。
     */
    fun loginFailed(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = message
        )
    }

    /**
     * 清除当前登录状态。
     */
    fun logout() {
        repository.logout()
        _uiState.value = UserUiState()
    }
}
