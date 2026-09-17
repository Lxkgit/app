package com.blog.app.data.repository

import com.blog.app.core.network.NetworkModule
import com.blog.app.data.api.ArticleApi
import com.blog.app.data.model.article.Article
import com.blog.app.data.model.article.ArticleLabel
import com.blog.app.data.model.article.ArticlePage
import com.blog.app.data.model.article.ArticleType
import com.blog.app.data.model.article.ArticleTypeSummary
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Repository for the blog article and category APIs.
 */
class ArticleRepository(
    private val api: ArticleApi = NetworkModule.create(ArticleApi::class.java)
) {
    /**
     * Loads the home article feed without category filtering.
     */
    suspend fun getArticles(pageNum: Int, pageSize: Int): ArticlePage =
        getArticles(pageNum, pageSize, null)

    /**
     * Loads articles with an optional category filter.
     */
    suspend fun getArticles(pageNum: Int, pageSize: Int, articleType: Long?): ArticlePage {
        val response = api.getArticles(
            pageNum = pageNum,
            pageSize = pageSize,
            type = 0L,
            selectUser = 0,
            selectStatus = "1,2",
            sortType = "0,1",
            articleType = articleType
        )
        val result = response.objectValue("result")
        val list = result.arrayValue("list")?.map(::parseArticle).orEmpty()
        return ArticlePage(
            page = result.int("page", pageNum),
            size = result.int("size", pageSize),
            total = result.long("total", list.size.toLong()),
            list = list
        )
    }

    /**
     * Loads the complete article category tree.
     */
    suspend fun getArticleTypes(): List<ArticleType> {
        val response = api.getArticleTypes()
        return response.arrayValue("result")?.map(::parseType).orEmpty()
    }

    private fun parseArticle(element: JsonElement): Article {
        val value = element as? JsonObject ?: JsonObject(emptyMap())
        return Article(
            id = value.long("id", 0L),
            userId = value.long("userId", 0L),
            title = value.string("title"),
            contentMd = value.string("contentMd"),
            contentImg = value.stringOrNull("contentImg"),
            contentMemo = value.string("contentMemo"),
            articleFile = value.stringOrNull("articleFile"),
            articleType = value.string("articleType"),
            articleLabel = value.string("articleLabel"),
            articleStatus = value.int("articleStatus", 0),
            browseCount = value.long("browseCount", 0L),
            likeCount = value.long("likeCount", 0L),
            createTime = value.string("createTime"),
            updateTime = value.string("updateTime"),
            articleTypes = value.arrayValue("articleTypes")?.map(::parseTypeSummary).orEmpty(),
            articleLabels = value.arrayValue("articleLabels")?.map(::parseLabel).orEmpty()
        )
    }

    private fun parseType(element: JsonElement): ArticleType {
        val value = element as? JsonObject ?: JsonObject(emptyMap())
        return ArticleType(
            id = value.long("id", 0L),
            parentId = value.long("parentId", 0L),
            typeName = value.string("typeName"),
            num = value.int("num", 0),
            node = value.intOrNull("node"),
            createUser = value.long("createUser", 0L),
            createTime = value.string("createTime"),
            updateTime = value.string("updateTime"),
            value = value.string("value"),
            label = value.string("label"),
            children = value.arrayValue("children")?.map(::parseType).orEmpty()
        )
    }

    private fun parseTypeSummary(element: JsonElement): ArticleTypeSummary {
        val value = element as? JsonObject ?: JsonObject(emptyMap())
        return ArticleTypeSummary(
            id = value.long("id", 0L),
            parentId = value.long("parentId", 0L),
            typeName = value.string("typeName")
        )
    }

    private fun parseLabel(element: JsonElement): ArticleLabel {
        val value = element as? JsonObject ?: JsonObject(emptyMap())
        return ArticleLabel(
            id = value.long("id", 0L),
            userId = value.long("userId", 0L),
            labelType = value.int("labelType", 0),
            labelName = value.string("labelName"),
            articleNum = value.int("articleNum", 0)
        )
    }

    private fun JsonElement.objectValue(name: String): JsonObject =
        (this as? JsonObject)?.get(name) as? JsonObject ?: JsonObject(emptyMap())

    private fun JsonElement.arrayValue(name: String): JsonArray? =
        (this as? JsonObject)?.get(name) as? JsonArray

    private fun JsonObject.arrayValue(name: String): JsonArray? =
        this[name] as? JsonArray

    private fun JsonObject.string(name: String): String =
        this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun JsonObject.stringOrNull(name: String): String? =
        this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull

    private fun JsonObject.long(name: String, default: Long): Long =
        this[name]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: default

    private fun JsonObject.int(name: String, default: Int): Int =
        this[name]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: default

    private fun JsonObject.intOrNull(name: String): Int? =
        this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull?.toIntOrNull()
}
