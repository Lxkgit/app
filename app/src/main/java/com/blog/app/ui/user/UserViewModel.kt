package com.blog.app.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blog.app.core.storage.AuthStorage
import com.blog.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the personal page authentication flow.
 */
data class UserUiState(
    val loggedIn: Boolean = AuthStorage.isLoggedIn(),
    val username: String = AuthStorage.username(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Coordinates login and logout for the personal page.
 */
class UserViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    /**
     * Logs in through the existing blog authentication service.
     */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.login(username.trim(), password) }
                .onSuccess {
                    _uiState.value = UserUiState(
                        loggedIn = true,
                        username = username.trim()
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "登录失败"
                    )
                }
        }
    }

    /**
     * Clears the current login state.
     */
    fun logout() {
        repository.logout()
        _uiState.value = UserUiState()
    }
}
