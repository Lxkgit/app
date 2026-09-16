package com.blog.app.ui.article

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin

/**
 * Displays an article and renders its Markdown body.
 */
@Composable
fun ArticleDetailScreen(article: Article) {
    val context = LocalContext.current
    val markwon = Markwon.builder(context)
        .usePlugin(TablePlugin.create(context))
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(StrikethroughPlugin.create())
        .build()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
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
                modifier = Modifier.fillMaxSize(),
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
