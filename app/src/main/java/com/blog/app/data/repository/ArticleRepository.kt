package com.blog.app.data.repository

import com.blog.app.core.network.NetworkModule
import com.blog.app.data.api.ArticleApi
import com.blog.app.data.model.article.Article
import com.blog.app.data.model.article.ArticlePage
import com.blog.app.data.model.article.ArticleType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Repository for blog article and category data.
 */
class ArticleRepository(
    private val api: ArticleApi = NetworkModule.create(ArticleApi::class.java)
) {
    /**
     * Loads one page of public articles using the website's default filters.
     */
    suspend fun getArticles(pageNum: Int, pageSize: Int): ArticlePage {
        val response = api.getArticles(
            pageNum = pageNum,
            pageSize = pageSize,
            type = 0,
            selectUser = 0,
            selectStatus = "1,2",
            sortType = "0,1"
        )
        val page = unwrap(response)
        val records = findArray(page, "records", "list", "rows", "items")
            ?.map(::parseArticle)
            .orEmpty()
        val current = page.long("current", page.long("pageNum", pageNum.toLong())).toInt()
        val size = page.long("size", page.long("pageSize", pageSize.toLong())).toInt()
        val total = page.long("total", records.size.toLong())
        val pages = page.long("pages", if (size > 0) ((total + size - 1) / size) else 0L).toInt()
        return ArticlePage(records, total, current, size, pages)
    }

    /**
     * Loads the article category tree.
     */
    suspend fun getArticleTypes(): List<ArticleType> {
        val response = api.getArticleTypes()
        val array = findArray(unwrap(response), "records", "list", "rows", "items")
            ?: response.asArrayOrNull()
            ?: return emptyList()
        return array.map(::parseType)
    }

    private fun unwrap(element: JsonElement): JsonObject {
        val root = element as? JsonObject ?: return JsonObject(emptyMap())
        val data = root["data"]
        return data as? JsonObject ?: root
    }

    private fun findArray(element: JsonElement, vararg names: String): JsonArray? {
        val objectValue = element as? JsonObject ?: return null
        names.forEach { name ->
            val value = objectValue[name]
            if (value is JsonArray) {
                return value
            }
        }
        return null
    }

    private fun parseArticle(element: JsonElement): Article {
        val value = element as? JsonObject ?: JsonObject(emptyMap())
        return Article(
            id = value.long("id", value.long("articleId", 0L)),
            title = value.string("title", value.string("articleTitle", "无标题")),
            content = value.string("content", value.string("articleContent", value.string("markdown", ""))),
            summary = value.string("summary", value.string("description", "")),
            createTime = value.string("createTime", value.string("createdAt", "")),
            updateTime = value.string("updateTime", value.string("updatedAt", "")),
            typeId = value.long("typeId", value.long("type", 0L)),
            typeName = value.string("typeName", value.string("categoryName", "")),
            authorName = value.string("authorName", value.string("username", value.string("userName", ""))),
            cover = value.stringOrNull("cover", value.stringOrNull("coverUrl", value.stringOrNull("image")))
        )
    }

    private fun parseType(element: JsonElement): ArticleType {
        val value = element as? JsonObject ?: JsonObject(emptyMap())
        val children = value["children"]?.let { it as? JsonArray }?.map(::parseType).orEmpty()
        return ArticleType(
            id = value.long("id", value.long("key", 0L)),
            name = value.string("name", value.string("title", value.string("label", "未命名"))),
            children = children
        )
    }

    private fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray

    private fun JsonObject.string(name: String, default: String): String =
        this[name]?.jsonPrimitive?.contentOrNull ?: default

    private fun JsonObject.stringOrNull(name: String): String? =
        this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull

    private fun JsonObject.long(name: String, default: Long): Long =
        this[name]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: default
}
