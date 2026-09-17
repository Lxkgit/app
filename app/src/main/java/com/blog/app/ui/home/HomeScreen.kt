package com.blog.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blog.app.data.model.article.Article
import com.blog.app.ui.common.ArticleCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Blog application home screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onArticleClick: (Article) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("首页") }) }) { padding ->
        when {
            state.isLoading && state.articles.isEmpty() -> LoadingView(Modifier.padding(padding))
            state.errorMessage != null && state.articles.isEmpty() -> ErrorView(
                modifier = Modifier.padding(padding), message = state.errorMessage, onRetry = viewModel::retry
            )
            state.articles.isEmpty() -> EmptyView(Modifier.padding(padding))
            else -> ArticleList(
                modifier = Modifier.padding(padding), state = state,
                onArticleClick = onArticleClick, onLoadMore = viewModel::loadNextPage
            )
        }
    }
}

/**
 * Displays the paginated home article feed.
 */
@Composable
private fun ArticleList(
    modifier: Modifier, state: HomeUiState, onArticleClick: (Article) -> Unit, onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.articles.size, state.hasMore, state.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { it to state.articles.lastIndex }.distinctUntilChanged()
            .filter { (lastIndex, lastArticleIndex) ->
                state.hasMore && !state.isLoadingMore && lastArticleIndex >= 0 && lastIndex >= lastArticleIndex - 1
            }.collect { onLoadMore() }
    }
    LazyColumn(
        state = listState, modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.articles, key = { it.id }) { article -> ArticleCard(article, onArticleClick) }
        if (state.isLoadingMore) item {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * Displays the loading state.
 */
@Composable
private fun LoadingView(modifier: Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
    }
}

/**
 * Displays the empty state.
 */
@Composable
private fun EmptyView(modifier: Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("暂时没有文章", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Displays an article loading error.
 */
@Composable
private fun ErrorView(modifier: Modifier, message: String?, onRetry: () -> Unit) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(message ?: "文章加载失败", modifier = Modifier.padding(24.dp))
        Button(onClick = onRetry, modifier = Modifier.padding(horizontal = 24.dp)) { Text("重试") }
    }
}
