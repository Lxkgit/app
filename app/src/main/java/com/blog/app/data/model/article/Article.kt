package com.blog.app.data.model.article

/**
 * Article model returned by the blog content service.
 */
data class Article(
    val id: Long,
    val userId: Long,
    val title: String,
    val contentMd: String,
    val contentImg: String?,
    val contentMemo: String,
    val articleFile: String?,
    val articleType: String,
    val articleLabel: String,
    val articleStatus: Int,
    val browseCount: Long,
    val likeCount: Long,
    val createTime: String,
    val updateTime: String,
    val articleTypes: List<ArticleTypeSummary>,
    val articleLabels: List<ArticleLabel>
)

/**
 * Article category information embedded in an article response.
 */
data class ArticleTypeSummary(
    val id: Long,
    val parentId: Long,
    val typeName: String
)

/**
 * Article label information embedded in an article response.
 */
data class ArticleLabel(
    val id: Long,
    val userId: Long,
    val labelType: Int,
    val labelName: String,
    val articleNum: Int
)
