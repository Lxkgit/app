package com.blog.app.ui.article

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.blog.app.data.model.article.Article
import com.bumptech.glide.Glide
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin

/**
 * 显示文章并渲染 Markdown 正文。
 */
@Composable
fun ArticleDetailScreen(article: Article) {
    val context = LocalContext.current
    val markwon = Markwon.builder(context)
        .usePlugin(TablePlugin.create(context))
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(GlideImagesPlugin.create(Glide.with(context)))
        .build()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        article.contentImg?.takeIf { it.isNotBlank() }?.let { imageUrl ->
            AsyncImage(
                model = if (imageUrl.startsWith("http")) imageUrl else "http://124.221.195.130" + imageUrl,
                contentDescription = article.title,
                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = buildString {
                if (article.typeName.isNotBlank()) append(article.typeName)
                if (article.createTime.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(article.createTime)
                }
                if (article.authorName.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(article.authorName)
                }
            },
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            style = MaterialTheme.typography.bodySmall
        )
        if (article.content.isBlank()) {
            Text(
                text = "文章暂无正文",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = {
                    TextView(it).apply {
                        textSize = 16f
                        setTextIsSelectable(true)
                        movementMethod = LinkMovementMethod.getInstance()
                    }
                },
                update = { textView ->
                    markwon.setMarkdown(textView, article.content)
                }
            )
        }
    }
}
