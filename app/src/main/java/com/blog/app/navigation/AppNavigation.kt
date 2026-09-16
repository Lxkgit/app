package com.blog.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import com.blog.app.data.model.article.Article
import com.blog.app.ui.article.ArticleDetailScreen
import com.blog.app.ui.home.HomeScreen

/**
 * Application navigation entry point.
 */
@Composable
fun AppNavigation() {
    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    val article = selectedArticle

    if (article == null) {
        HomeScreen(onArticleClick = { selectedArticle = it })
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("文章") },
                    navigationIcon = {
                        TextButton(onClick = { selectedArticle = null }) {
                            Text("返回")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize()) {
                ArticleDetailScreen(article)
            }
        }
    }
}
