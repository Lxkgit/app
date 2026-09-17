package com.blog.app.data.api

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 博客用户接口。
 */
interface UserApi {
    /**
     * 根据用户名获取当前用户资料。
     */
    @GET("user/username")
    suspend fun getUserByUsername(
        @Query("username") username: String
    ): JsonElement
}
