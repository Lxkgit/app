package com.blog.app.data.repository

import com.blog.app.core.network.NetworkModule
import com.blog.app.core.storage.AuthStorage
import com.blog.app.data.api.content.UserApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * 用户资料数据仓库。
 */
class UserRepository(
    private val api: UserApi = NetworkModule.createAuth(UserApi::class.java)
) {
    /**
     * 获取当前登录用户的资料。
     */
    suspend fun getCurrentUser(): UserProfile? {
        val username = AuthStorage.username()
        if (username.isBlank()) {
            return null
        }
        val response = api.getUserByUsername(username)
        val result = (response as? JsonObject)?.get("result") as? JsonObject ?: return null
        return UserProfile(
            username = result.string("username").ifBlank { username },
            avatar = result.firstString("avatar", "userAvatar", "headImg", "headUrl", "avatarUrl")
        )
    }

    private fun JsonObject.string(key: String): String =
        this[key]?.jsonPrimitive?.content.orEmpty()

    private fun JsonObject.firstString(vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key ->
            string(key).takeIf { it.isNotBlank() }
        }.orEmpty()
    }
}

/**
 * 用户资料。
 */
data class UserProfile(
    val username: String,
    val avatar: String
)
