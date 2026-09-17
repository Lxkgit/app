package com.blog.app.ui.user

import androidx.lifecycle.ViewModel
import com.blog.app.core.storage.AuthStorage
import com.blog.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
 * Coordinates OAuth2 login and logout for the personal page.
 */
class UserViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    /**
     * Refreshes the displayed login state from persistent storage.
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
     * Marks the authorization flow as loading.
     */
    fun beginLogin() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
    }

    /**
     * Displays an authorization error.
     */
    fun loginFailed(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = message
        )
    }

    /**
     * Clears the current login state.
     */
    fun logout() {
        repository.logout()
        _uiState.value = UserUiState()
    }
}
