package com.blog.app.data.model.article

/**
 * 博客内容服务返回的文章模型。
 */
data class Article(
    val id: Long,
    val userId: Long,
    val title: String,
    val contentMd: String,
    val contentImg: String?,
    val contentMemo: String,
    val articleFile: String?,
    val articleType: String,
    val articleLabel: String,
    val articleStatus: Int,
    val browseCount: Long,
    val likeCount: Long,
    val createTime: String,
    val updateTime: String,
    val articleTypes: List<ArticleTypeSummary>,
    val articleLabels: List<ArticleLabel>
) {
    /**
     * 提供给界面使用的 Markdown 正文。
     */
    val content: String
        get() = contentMd

    /**
     * 将文章分类名称组合为旧界面使用的显示文本。
     */
    val typeName: String
        get() = articleTypes.joinToString(" · ") { it.typeName }

    /**
     * 当前文章接口的列表数据没有提供作者名称。
     */
    val authorName: String
        get() = ""
}

/**
 * 文章响应中包含的分类信息。
 */
data class ArticleTypeSummary(
    val id: Long,
    val parentId: Long,
    val typeName: String
)

/**
 * 文章响应中包含的标签信息。
 */
data class ArticleLabel(
    val id: Long,
    val userId: Long,
    val labelType: Int,
    val labelName: String,
    val articleNum: Int
)
