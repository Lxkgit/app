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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.Alignment
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
 * 文章浏览页面，支持可折叠的三级分类树。
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
 * 显示分类树，父级和子级都支持直接筛选。
 */
@Composable
private fun CategoryTree(
    categories: List<ArticleType>, selectedType: Long?, onSelect: (ArticleType) -> Unit
) {
    var expandedIds by remember { mutableStateOf(emptySet<Long>()) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryNode(
                    category = category,
                    level = 0,
                    selectedType = selectedType,
                    expandedIds = expandedIds,
                    onToggle = { id -> expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id },
                    onSelect = onSelect
                )
            }
        }
    }
}

/**
 * 递归显示分类节点，点击节点本身进行筛选，展开按钮单独控制子分类。
 */
@Composable
private fun CategoryNode(
    category: ArticleType, level: Int, selectedType: Long?, expandedIds: Set<Long>,
    onToggle: (Long) -> Unit, onSelect: (ArticleType) -> Unit
) {
    val hasChildren = category.children.isNotEmpty()
    val expanded = category.id in expandedIds
    val selected = selectedType == category.id
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onSelect(category) }
                .padding(start = (8 + level * 22).dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasChildren) {
                IconButton(onClick = { onToggle(category.id) }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收起分类" else "展开分类"
                    )
                }
            } else {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(24.dp))
            }
            Text(
                text = category.typeName,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }
    }
    if (expanded) category.children.forEach { child ->
        CategoryNode(child, level + 1, selectedType, expandedIds, onToggle, onSelect)
    }
}

/**
 * 显示分页文章列表。
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
 * 显示加载状态。
 */
@Composable
private fun LoadingView() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
    }
}

/**
 * 显示空数据状态。
 */
@Composable
private fun EmptyView() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("暂时没有文章", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * 显示文章加载错误。
 */
@Composable
private fun ErrorView(message: String?, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(message ?: "文章加载失败", modifier = Modifier.padding(24.dp))
        Button(onClick = onRetry, modifier = Modifier.padding(horizontal = 24.dp)) { Text("重试") }
    }
}
