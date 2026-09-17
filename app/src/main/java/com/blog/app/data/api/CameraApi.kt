package com.blog.app.data.api

import kotlinx.serialization.json.JsonElement
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 摄像头授权接口。
 */
interface CameraApi {
    /**
     * 获取摄像头临时播放 Token。
     */
    @POST("camera/token")
    suspend fun createToken(
        @Query("stream") stream: String
    ): JsonElement
}
