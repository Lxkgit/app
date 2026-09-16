package com.blog.app.data.model.article

/**
 * Article category tree node returned by the blog service.
 */
data class ArticleType(
    val id: Long,
    val parentId: Long,
    val typeName: String,
    val num: Int,
    val node: Int?,
    val createUser: Long,
    val createTime: String,
    val updateTime: String,
    val value: String,
    val label: String,
    val children: List<ArticleType> = emptyList()
)
