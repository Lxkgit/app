package com.blog.app.data.api

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 博客文章内容服务接口。
 */
interface ArticleApi {
    /**
     * 获取文章分类树。
     */
    @GET("article/type/tree")
    suspend fun getArticleTypes(): JsonElement

    /**
     * 获取分页文章列表。
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
