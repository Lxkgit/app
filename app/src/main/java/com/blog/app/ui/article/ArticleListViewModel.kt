package com.blog.app.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blog.app.data.model.article.Article
import com.blog.app.data.model.article.ArticleType
import com.blog.app.data.repository.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 文章浏览页面的界面状态。
 */
data class ArticleListUiState(
    val articles: List<Article> = emptyList(),
    val categories: List<ArticleType> = emptyList(),
    val selectedType: Long? = null,
    val selectedTypeName: String = "全部分类",
    val isCategoryLoading: Boolean = true,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val errorMessage: String? = null,
    val page: Int = 0
)

/**
 * 加载文章列表，并通过 articleType 应用当前分类筛选。
 */
class ArticleListViewModel(
    private val repository: ArticleRepository = ArticleRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArticleListUiState())
    val uiState: StateFlow<ArticleListUiState> = _uiState.asStateFlow()

    private val pageSize = 5

    init {
        loadCategories()
        loadFirstPage()
    }

    /**
     * 加载完整的三级分类树。
     */
    private fun loadCategories() {
        viewModelScope.launch {
            runCatching { repository.getArticleTypes() }.onSuccess { categories ->
                    _uiState.value = _uiState.value.copy(
                        categories = categories, isCategoryLoading = false
                    )
                }.onFailure {
                    _uiState.value = _uiState.value.copy(isCategoryLoading = false)
                }
        }
    }

    /**
     * 根据当前分类加载第一页文章。
     */
    fun loadFirstPage() {
        viewModelScope.launch {
            val selectedType = _uiState.value.selectedType
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isLoadingMore = false,
                errorMessage = null,
                page = 0,
                articles = emptyList(),
                hasMore = true
            )
            runCatching {
                repository.getArticles(1, pageSize, selectedType)
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
                    isLoading = false, errorMessage = error.message ?: "文章加载失败"
                )
            }
        }
    }

    /**
     * 选择分类节点并重新加载文章列表。
     */
    fun selectCategory(category: ArticleType?) {
        val typeId = category?.id
        if (_uiState.value.selectedType == typeId) {
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedType = typeId, selectedTypeName = category?.typeName ?: "全部分类"
        )
        loadFirstPage()
    }

    /**
     * 加载下一页文章。
     */
    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore || state.page == 0) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true, errorMessage = null)
            runCatching {
                repository.getArticles(state.page + 1, pageSize, state.selectedType)
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    articles = _uiState.value.articles + result.list,
                    isLoadingMore = false,
                    hasMore = result.hasNext() && result.list.isNotEmpty(),
                    page = result.page
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false, errorMessage = error.message ?: "更多文章加载失败"
                )
            }
        }
    }

    /**
     * 重新请求当前分类的数据。
     */
    fun retry() {
        loadFirstPage()
    }
}
