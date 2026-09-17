package com.blog.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blog.app.data.model.article.Article
import com.blog.app.data.repository.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the blog home screen.
 */
data class HomeUiState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val errorMessage: String? = null,
    val page: Int = 0
)

/**
 * Loads and manages the home article feed.
 */
class HomeViewModel(
    private val repository: ArticleRepository = ArticleRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val pageSize = 5

    init {
        loadFirstPage()
    }

    /**
     * Loads the first page from the normal home article endpoint.
     */
    fun loadFirstPage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isLoadingMore = false,
                errorMessage = null,
                page = 0,
                articles = emptyList(),
                hasMore = true
            )
            runCatching {
                repository.getArticles(1, pageSize)
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    articles = result.list,
                    isLoading = false,
                    hasMore = result.hasNext(),
                    page = result.page,
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "文章加载失败"
                )
            }
        }
    }

    /**
     * Loads and appends the next server page.
     */
    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore || state.page == 0) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true, errorMessage = null)
            runCatching {
                repository.getArticles(state.page + 1, pageSize)
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    articles = _uiState.value.articles + result.list,
                    isLoadingMore = false,
                    hasMore = result.hasNext() && result.list.isNotEmpty(),
                    page = result.page,
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    errorMessage = error.message ?: "更多文章加载失败"
                )
            }
        }
    }

    /**
     * Retries the first page request.
     */
    fun retry() {
        loadFirstPage()
    }
}
