package com.blog.app.data.model.article

/**
 * Article category tree node.
 */
data class ArticleType(
    val id: Long,
    val name: String,
    val children: List<ArticleType> = emptyList()
)
