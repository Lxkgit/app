package com.blog.app.data.repository

import com.blog.app.core.network.NetworkModule
import com.blog.app.data.api.FileApi
import com.blog.app.data.model.file.FileDirectory
import com.blog.app.data.model.file.FileItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * 文件云盘数据仓库。
 */
class FileRepository(
    private val api: FileApi = NetworkModule.createFile(FileApi::class.java)
) {
    /**
     * 查询当前目录下的目录和文件。
     */
    suspend fun list(path: String?): FileListResult {
        val directoryResponse = api.selectDirectories(path)
        val fileResponse = api.selectFiles(path)
        return FileListResult(
            directories = directoryResponse.toResultArray().mapNotNull { it.toDirectory() },
            files = fileResponse.toResultArray().mapNotNull { it.toFile() }
        )
    }

    /**
     * 创建目录。
     */
    suspend fun createDirectory(path: String?, name: String, type: Int = 0) {
        api.createDirectory(
            buildJsonObject {
                put("dirPath", path.orEmpty())
                put("dirName", name)
                put("dirType", type)
            }
        )
    }

    /**
     * 删除目录。
     */
    suspend fun deleteDirectory(path: String, name: String) {
        api.deleteDirectory(path, name)
    }

    /**
     * 删除文件。
     */
    suspend fun deleteFiles(ids: List<Int>) {
        if (ids.isNotEmpty()) {
            api.deleteFiles(ids)
        }
    }

    private fun JsonElement.toResultArray(): JsonArray {
        val result = (this as? JsonObject)?.get("result") ?: return JsonArray(emptyList())
        return result as? JsonArray ?: JsonArray(emptyList())
    }

    private fun JsonElement.toDirectory(): FileDirectory? {
        val value = this as? JsonObject ?: return null
        return FileDirectory(
            id = value.int("id"),
            name = value.firstString("dirName", "name"),
            path = value.string("dirPath"),
            type = value.int("dirType")
        )
    }

    private fun JsonElement.toFile(): FileItem? {
        val value = this as? JsonObject ?: return null
        return FileItem(
            id = value.int("id"),
            name = value.firstString("fileName", "name"),
            url = value.string("fileUrl"),
            type = value.string("fileType"),
            size = value.long("fileSize"),
            status = value.intOrNull("fileStatus")
        )
    }

    private fun JsonObject.string(key: String): String =
        this[key]?.jsonPrimitive?.content.orEmpty()

    private fun JsonObject.firstString(vararg keys: String): String =
        keys.firstNotNullOfOrNull { key -> string(key).takeIf { it.isNotBlank() } }.orEmpty()

    private fun JsonObject.int(key: String): Int =
        this[key]?.jsonPrimitive?.intOrNull ?: 0

    private fun JsonObject.intOrNull(key: String): Int? =
        this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.long(key: String): Long =
        this[key]?.jsonPrimitive?.longOrNull ?: 0L
}

/**
 * 当前目录的数据。
 */
data class FileListResult(
    val directories: List<FileDirectory>,
    val files: List<FileItem>
)
