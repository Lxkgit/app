# Blog App

博客移动端 App，当前为 Android + Kotlin + Jetpack Compose 基础工程。

## 当前状态

- Android 原生项目已初始化
- Kotlin
- Jetpack Compose
- Material 3
- Java 17
- minSdk 26
- targetSdk 35
- applicationId：`com.lxkgit.blogapp`

## 后续规划

1. 登录与注册
2. 博客首页
3. 文章列表与详情
4. 分类、标签与搜索
5. 评论
6. 用户中心
7. 与博客现有后端 API 对接
8. 网络层、Token、缓存和异常处理

## 开发说明

项目代码以便于长期维护为目标进行组织。类、方法和模块会保持职责清晰，后续功能按业务模块逐步拆分。

> 注意：Kotlin 不支持使用 `#` 作为注释语法，因此 Kotlin 方法内部注释使用 `//`；方法、类等声明外部使用 `/** */` 文档注释。
