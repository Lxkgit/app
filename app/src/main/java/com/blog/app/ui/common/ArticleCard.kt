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
import androidx.compose.material3.CardDefaults
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
 * 首页和文章列表共用的紧凑文章卡片。
 */
@Composable
fun ArticleCard(
    article: Article,
    onClick: (Article) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(article) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            article.contentImg?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                AsyncImage(
                    model = resolveImageUrl(imageUrl),
                    contentDescription = article.title,
                    modifier = Modifier.height(92.dp).weight(0.34f),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .weight(if (article.contentImg.isNullOrBlank()) 1f else 0.66f)
                    .padding(start = if (article.contentImg.isNullOrBlank()) 0.dp else 12.dp)
            ) {
                Text(article.title, maxLines = 2, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (article.contentMemo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(article.contentMemo, maxLines = 2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(7.dp))
                Row {
                    if (article.typeName.isNotBlank()) {
                        Text(article.typeName, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("  ·  " + article.createTime, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * 将内容服务返回的相对图片路径转换为完整地址。
 */
private fun resolveImageUrl(url: String): String =
    if (url.startsWith("http://") || url.startsWith("https://")) url else "http://124.221.195.130" + url
