package com.blog.app.data.api

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Blog article content service API.
 */
interface ArticleApi {
    /**
     * Loads the article category tree.
     */
    @GET("article/type/tree")
    suspend fun getArticleTypes(): JsonElement

    /**
     * Loads a paginated article list.
     */
    @GET("article/list")
    suspend fun getArticles(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int,
        @Query("type") type: Long,
        @Query("selectUser") selectUser: Int,
        @Query("selectStatus") selectStatus: String,
        @Query("sortType") sortType: String,
        @Query("articleType") articleType: Long? = null
    ): JsonElement
}
