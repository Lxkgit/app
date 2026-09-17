package com.blog.app.data.model.article

/**
 * 博客服务返回的文章分类树节点。
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
