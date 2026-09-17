package com.blog.app.ui.article

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blog.app.data.model.article.Article
import com.blog.app.data.model.article.ArticleType
import com.blog.app.ui.common.ArticleCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Article browsing screen with a collapsible three-level category tree.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (Article) -> Unit,
    viewModel: ArticleListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var categoryExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("文章") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedType == null,
                    onClick = { viewModel.selectCategory(null); categoryExpanded = false },
                    label = { Text("全部分类") }
                )
                FilterChip(
                    selected = state.selectedType != null,
                    onClick = { categoryExpanded = !categoryExpanded },
                    label = { Text(state.selectedTypeName) }
                )
            }

            if (categoryExpanded) {
                CategoryTree(
                    categories = state.categories,
                    selectedType = state.selectedType,
                    onSelect = { viewModel.selectCategory(it); categoryExpanded = false }
                )
            }

            when {
                state.isLoading && state.articles.isEmpty() -> LoadingView()
                state.errorMessage != null && state.articles.isEmpty() -> ErrorView(state.errorMessage, viewModel::retry)
                state.articles.isEmpty() -> EmptyView()
                else -> ArticleList(state, onArticleClick, viewModel::loadNextPage)
            }
        }
    }
}

/**
 * Displays the category tree with independent expansion state for each node.
 */
@Composable
private fun CategoryTree(
    categories: List<ArticleType>, selectedType: Long?, onSelect: (ArticleType) -> Unit
) {
    var expandedIds by remember { mutableStateOf(emptySet<Long>()) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryNode(category, 0, selectedType, expandedIds,
                    { id -> expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id }, onSelect)
            }
        }
    }
}

/**
 * Renders one category node and its descendants recursively.
 */
@Composable
private fun CategoryNode(
    category: ArticleType, level: Int, selectedType: Long?, expandedIds: Set<Long>,
    onToggle: (Long) -> Unit, onSelect: (ArticleType) -> Unit
) {
    val hasChildren = category.children.isNotEmpty()
    val expanded = category.id in expandedIds
    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            if (hasChildren) onToggle(category.id) else onSelect(category)
        }.padding(start = (16 + level * 22).dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (!hasChildren) "•" else if (expanded) "⌄" else "›")
        Text(
            category.typeName,
            modifier = Modifier.weight(1f),
            fontWeight = if (selectedType == category.id) FontWeight.Bold else FontWeight.Normal
        )
    }
    if (expanded) category.children.forEach { child ->
        CategoryNode(child, level + 1, selectedType, expandedIds, onToggle, onSelect)
    }
}

/**
 * Displays the paginated article list.
 */
@Composable
private fun ArticleList(
    state: ArticleListUiState, onArticleClick: (Article) -> Unit, onLoadMore: () -> Unit
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
        state = listState, modifier = Modifier.fillMaxSize(),
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
private fun LoadingView() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
    }
}

/**
 * Displays the empty state.
 */
@Composable
private fun EmptyView() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("暂时没有文章", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Displays a loading error.
 */
@Composable
private fun ErrorView(message: String?, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(message ?: "文章加载失败", modifier = Modifier.padding(24.dp))
        Button(onClick = onRetry, modifier = Modifier.padding(horizontal = 24.dp)) { Text("重试") }
    }
}
