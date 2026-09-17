package com.blog.app.data.model.article

/**
 * 博客文章服务返回的分页结果。
 */
data class ArticlePage(
    val page: Int,
    val size: Int,
    val total: Long,
    val list: List<Article>
) {
    /**
     * 根据服务器返回的总数量判断是否还有下一页。
     */
    fun hasNext(): Boolean = page * size.toLong() < total
}
