package com.blog.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.blog.app.R
import com.blog.app.data.model.article.Article
import com.blog.app.ui.article.ArticleDetailScreen
import com.blog.app.ui.article.ArticleListScreen
import com.blog.app.ui.home.HomeScreen
import com.blog.app.ui.user.UserScreen

/**
 * 应用主导航入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    onLogin: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedArticle by remember { mutableStateOf<Article?>(null) }

    if (selectedArticle != null) {
        val goBack = { selectedArticle = null }
        BackHandler(onBack = goBack)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("文章详情") },
                    navigationIcon = {
                        IconButton(onClick = goBack) {
                            Icon(
                                painter = painterResource(R.drawable.icon_back),
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                ArticleDetailScreen(selectedArticle!!)
            }
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(modifier = Modifier.height(56.dp)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(painterResource(R.drawable.icon_home), "首页") },
                    label = null
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(painterResource(R.drawable.icon_article), "文章") },
                    label = null
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(painterResource(R.drawable.icon_user), "我的") },
                    label = null
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(onArticleClick = { selectedArticle = it })
                1 -> ArticleListScreen(onArticleClick = { selectedArticle = it })
                2 -> UserScreen(onLogin = onLogin)
            }
        }
    }
}
