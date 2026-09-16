package com.blog.app.data.model.article

/**
 * Paginated article result.
 */
data class ArticlePage(
    val records: List<Article>,
    val total: Long,
    val current: Int,
    val size: Int,
    val pages: Int
) {
    /**
     * Indicates whether another page can be requested.
     */
    fun hasNext(): Boolean = current < pages || records.size >= size
}
