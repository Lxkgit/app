package com.blog.app.data.model.menu

/**
 * 当前用户的权限菜单。
 */
data class UserMenu(
    val id: Long,
    val menuName: String,
    val menuIcon: String,
    val auth: String,
    val menuType: Int,
    val children: List<UserMenu> = emptyList()
)
