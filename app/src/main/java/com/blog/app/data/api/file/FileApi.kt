package com.blog.app.data.api.file

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 用户文件云盘接口。
 */
interface FileApi {
    /**
     * 查询当前目录下的子目录。
     */
    @GET("api/file/dir/select")
    suspend fun selectDirectories(
        @Query("dirPath") dirPath: String? = null
    ): JsonElement

    /**
     * 查询当前目录下的文件。
     */
    @GET("api/file/dir/select/file")
    suspend fun selectFiles(
        @Query("dirPath") dirPath: String? = null
    ): JsonElement

    /**
     * 创建目录。
     */
    @POST("api/file/dir/save")
    suspend fun createDirectory(
        @Body body: JsonObject
    ): JsonElement

    /**
     * 删除目录。
     */
    @DELETE("api/file/dir/delete")
    suspend fun deleteDirectory(
        @Query("dirPath") dirPath: String,
        @Query("dirName") dirName: String
    ): JsonElement

    /**
     * 删除文件。
     */
    @DELETE("api/file/dir/delete/file")
    suspend fun deleteFiles(
        @Query("idList") idList: List<Int>
    ): JsonElement
}
