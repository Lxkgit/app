package com.blog.app.data.model.file

/**
 * 文件云盘中的目录。
 */
data class FileDirectory(
    val id: Int,
    val name: String,
    val path: String,
    val type: Int = 0
)

/**
 * 文件云盘中的文件。
 */
data class FileItem(
    val id: Int,
    val name: String,
    val url: String = "",
    val type: String = "",
    val size: Long = 0L,
    val status: Int? = null
)
