package com.blog.app.data.model.article

/**
 * Blog article model used by the application UI.
 */
data class Article(
    val id: Long,
    val title: String,
    val content: String,
    val summary: String,
    val createTime: String,
    val updateTime: String,
    val typeId: Long,
    val typeName: String,
    val authorName: String,
    val cover: String?
)
