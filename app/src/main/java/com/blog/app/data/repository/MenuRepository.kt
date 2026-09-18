package com.blog.app.data.repository

import com.blog.app.data.api.MenuApi
import com.blog.app.data.model.menu.UserMenu
import com.blog.app.core.network.NetworkModule
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * 用户权限菜单数据仓库。
 */
class MenuRepository(
    private val api: MenuApi = NetworkModule.createAuth(MenuApi::class.java)
) {
    /**
     * 获取当前登录用户的权限菜单。
     */
    suspend fun getUserMenus(): List<UserMenu> {
        val response = api.getUserMenus()
        val result = response.asObject()?.get("result")
        return result?.asArray()?.mapNotNull { it.toMenu() }.orEmpty()
    }

    private fun JsonElement.asObject(): JsonObject? = this as? JsonObject

    private fun JsonElement.asArray(): JsonArray? = this as? JsonArray

    private fun JsonElement.toMenu(): UserMenu? {
        val objectValue = asObject() ?: return null
        return UserMenu(
            id = objectValue.long("id"),
            menuName = objectValue.string("menuName"),
            menuIcon = objectValue.string("menuIcon"),
            auth = objectValue.string("auth"),
            menuType = objectValue.int("menuType"),
            children = objectValue["children"]?.asArray()?.mapNotNull { it.toMenu() }.orEmpty()
        )
    }

    private fun JsonObject.string(key: String): String =
        this[key]?.jsonPrimitive?.content.orEmpty()

    private fun JsonObject.long(key: String): Long =
        this[key]?.jsonPrimitive?.longOrNull ?: 0L

    private fun JsonObject.int(key: String): Int =
        this[key]?.jsonPrimitive?.intOrNull ?: 0
}
