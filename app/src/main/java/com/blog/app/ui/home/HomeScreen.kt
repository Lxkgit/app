package com.blog.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blog.app.data.model.article.Article
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Blog application home screen.
 */
@Composable
fun HomeScreen(
    onArticleClick: (Article) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Blog") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CategoryBar(state, viewModel)
            when {
                state.isLoading && state.articles.isEmpty() -> LoadingView()
                state.errorMessage != null && state.articles.isEmpty() -> ErrorView(
                    message = state.errorMessage,
                    onRetry = viewModel::retry
                )
                state.articles.isEmpty() -> EmptyView()
                else -> ArticleList(
                    state = state,
                    onArticleClick = onArticleClick,
                    onLoadMore = viewModel::loadNextPage
                )
            }
        }
    }
}

/**
 * Displays the article category filters.
 */
@Composable
private fun CategoryBar(state: HomeUiState, viewModel: HomeViewModel) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = state.selectedType == 0L,
                onClick = { viewModel.selectType(0L) },
                label = { Text("全部") }
            )
        }
        items(state.categories, key = { it.id }) { category ->
            FilterChip(
                selected = state.selectedType == category.id,
                onClick = { viewModel.selectType(category.id) },
                label = { Text(category.name) }
            )
        }
    }
}

/**
 * Displays the paginated article feed.
 */
@Composable
private fun ArticleList(
    state: HomeUiState,
    onArticleClick: (Article) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, state.articles.size, state.hasMore, state.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { lastIndex -> lastIndex to state.articles.lastIndex }
            .distinctUntilChanged()
            .filter { (lastIndex, lastArticleIndex) ->
                state.hasMore && !state.isLoadingMore && lastArticleIndex >= 0 && lastIndex >= lastArticleIndex - 1
            }
            .collect {
                onLoadMore()
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = state.articles,
            key = { index, article -> if (article.id != 0L) article.id else "article-$index-${article.title}" }
        ) { _, article ->
            ArticleCard(article, onArticleClick)
        }
        if (state.isLoadingMore) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (!state.hasMore) {
            item {
                Text(
                    text = "没有更多文章了",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Displays one article summary card.
 */
@Composable
private fun ArticleCard(article: Article, onArticleClick: (Article) -> Unit) {
    val preview = if (article.summary.isNotBlank()) article.summary else stripMarkdown(article.content).take(160)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onArticleClick(article) },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                if (article.typeName.isNotBlank()) {
                    Text(article.typeName, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(article.createTime, style = MaterialTheme.typography.labelMedium)
            }
            if (preview.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = preview,
                    maxLines = 3,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Displays the initial loading state.
 */
@Composable
private fun LoadingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
    }
}

/**
 * Displays an empty article state.
 */
@Composable
private fun EmptyView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("暂时没有文章", style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Displays an article loading error.
 */
@Composable
private fun ErrorView(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(message ?: "文章加载失败", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

private fun stripMarkdown(markdown: String): String = markdown
    .replace(Regex("```[\\s\\S]*?```"), "")
    .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")
    .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
    .replace(Regex("[#>*_`~-]"), "")
    .replace(Regex("\\s+"), " ")
    .trim()
