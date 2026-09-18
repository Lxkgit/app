package com.blog.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.blog.app.data.model.article.Article
import com.blog.app.ui.common.ArticleCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * 博客应用首页。
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
            state.errorMessage != null && state.articles.isEmpty() -> ErrorView(Modifier.padding(padding), state.errorMessage, viewModel::retry)
            state.articles.isEmpty() -> EmptyView(Modifier.padding(padding))
            else -> HomeContent(Modifier.padding(padding), state, onArticleClick, viewModel::loadNextPage)
        }
    }
}

/**
 * 首页内容。
 */
@Composable
private fun HomeContent(
    modifier: Modifier,
    state: HomeUiState,
    onArticleClick: (Article) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.articles.size, state.hasMore, state.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { it to state.articles.lastIndex }
            .distinctUntilChanged()
            .filter { (lastIndex, lastArticleIndex) ->
                state.hasMore && !state.isLoadingMore && lastArticleIndex >= 0 && lastIndex >= lastArticleIndex - 1
            }
            .collect { onLoadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("推荐文章", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 2.dp)) }
        item { ArticleCarousel(state.articles.take(3), onArticleClick) }
        item { Text("最新文章", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp, start = 2.dp)) }
        items(state.articles, key = { it.id }) { article -> ArticleCard(article, onArticleClick) }
        if (state.isLoadingMore) item {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * 推荐文章轮播图，优先使用文章封面，没有封面时使用 LoliAPI 图片。
 */
@Composable
private fun ArticleCarousel(
    articles: List<Article>,
    onArticleClick: (Article) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(end = 30.dp),
        pageSpacing = 10.dp
    ) { page ->
        val article = articles.getOrNull(page)
        val imageUrl = article?.contentImg?.takeIf { it.isNotBlank() }
            ?: "https://www.loliapi.com/acg/?page=" + page

        Card(
            modifier = Modifier.fillMaxWidth().height(190.dp),
            shape = RoundedCornerShape(20.dp),
            onClick = { article?.let(onArticleClick) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = article?.title ?: "推荐图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)))
                    )
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                ) {
                    Text(
                        article?.title ?: "精选图片",
                        maxLines = 2,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    if (article != null && article.typeName.isNotBlank()) {
                        Text(article.typeName, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.82f), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
        repeat(3) { index ->
            Box(
                modifier = Modifier.padding(horizontal = 3.dp).height(5.dp).clip(RoundedCornerShape(50)).background(
                    if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

/**
 * 加载状态。
 */
@Composable
private fun LoadingView(modifier: Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
    }
}

/**
 * 空数据状态。
 */
@Composable
private fun EmptyView(modifier: Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("暂时没有文章", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * 文章加载错误。
 */
@Composable
private fun ErrorView(modifier: Modifier, message: String?, onRetry: () -> Unit) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(message ?: "文章加载失败", modifier = Modifier.padding(24.dp))
        Button(onClick = onRetry, modifier = Modifier.padding(horizontal = 24.dp)) { Text("重试") }
    }
}
