package com.blog.app.data.api.auth

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 博客权限菜单接口。
 */
interface MenuApi {
    /**
     * 获取当前登录用户的权限菜单。
     */
    @GET("auth/menu/list/user")
    suspend fun getUserMenus(
        @Query("menuType") menuType: Int = 2
    ): JsonElement
}
