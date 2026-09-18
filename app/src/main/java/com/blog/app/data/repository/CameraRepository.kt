package com.blog.app.data.repository

import com.blog.app.core.network.NetworkModule
import com.blog.app.data.api.CameraApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * 摄像头数据仓库。
 */
class CameraRepository(
    private val api: CameraApi = NetworkModule.createFile(CameraApi::class.java)
) {
    /**
     * 获取指定摄像头的临时播放 Token。
     */
    suspend fun createToken(stream: String): CameraToken {
        val response = api.createToken(stream)
        val result = (response as? JsonObject)?.get("result") as? JsonObject
            ?: throw IllegalStateException("获取摄像头 Token 失败")
        val token = result["token"]?.jsonPrimitive?.content.orEmpty()
        if (token.isBlank()) {
            throw IllegalStateException("摄像头 Token 为空")
        }
        return CameraToken(
            token = token,
            expiresIn = result["expiresIn"]?.jsonPrimitive?.longOrNull ?: 0L
        )
    }
}

/**
 * 摄像头临时播放 Token。
 */
data class CameraToken(
    val token: String,
    val expiresIn: Long
)
