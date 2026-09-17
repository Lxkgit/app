package com.blog.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.blog.app.data.model.article.Article

/**
 * 首页和文章列表共用的文章卡片。
 */
@Composable
fun ArticleCard(
    article: Article,
    onClick: (Article) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(article) },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            article.contentImg?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                AsyncImage(
                    model = resolveImageUrl(imageUrl),
                    contentDescription = article.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(article.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (article.contentMemo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(article.contentMemo, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
                }
                if (article.articleTypes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row {
                        Text(article.typeName, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "${article.createTime}  ·  浏览 ${article.browseCount}  ·  点赞 ${article.likeCount}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * 将内容服务返回的相对图片路径转换为完整地址。
 */
private fun resolveImageUrl(url: String): String =
    if (url.startsWith("http://") || url.startsWith("https://")) url
    else "http://124.221.195.130$url"
