package com.blog.app.data.model.article

/**
 * Paginated result returned by the blog article service.
 */
data class ArticlePage(
    val page: Int,
    val size: Int,
    val total: Long,
    val list: List<Article>
) {
    /**
     * Indicates whether another page is available according to the server total.
     */
    fun hasNext(): Boolean = page * size.toLong() < total
}
