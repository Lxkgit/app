package com.blog.app.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blog.app.core.storage.AuthStorage
import com.blog.app.data.model.menu.UserMenu
import com.blog.app.data.repository.AuthRepository
import com.blog.app.data.repository.MenuRepository
import com.blog.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 个人页面的界面状态。
 */
data class UserUiState(
    val loggedIn: Boolean = AuthStorage.isLoggedIn(),
    val username: String = AuthStorage.username(),
    val avatar: String = "",
    val menus: List<UserMenu> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 协调个人页面的用户资料、权限和登录流程。
 */
class UserViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val menuRepository: MenuRepository = MenuRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    /**
     * 刷新当前登录用户资料和权限。
     */
    fun refresh() {
        if (!AuthStorage.isLoggedIn()) {
            _uiState.value = UserUiState()
            return
        }
        _uiState.value = _uiState.value.copy(
            loggedIn = true,
            username = AuthStorage.username(),
            isLoading = true,
            errorMessage = null
        )
        viewModelScope.launch {
            runCatching {
                val profile = userRepository.getCurrentUser()
                val menus = menuRepository.getUserMenus()
                profile to menus
            }.onSuccess { (profile, menus) ->
                _uiState.value = _uiState.value.copy(
                    loggedIn = true,
                    username = profile?.username ?: AuthStorage.username(),
                    avatar = profile?.avatar.orEmpty(),
                    menus = menus,
                    isLoading = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    loggedIn = true,
                    isLoading = false,
                    errorMessage = error.message ?: "获取用户信息失败"
                )
            }
        }
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
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = message)
    }

    /**
     * 清除当前登录状态。
     */
    fun logout() {
        authRepository.logout()
        _uiState.value = UserUiState()
    }
}
