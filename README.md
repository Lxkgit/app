# Blog App

博客移动端 App，当前为 Android + Kotlin + Jetpack Compose 基础工程。

## 当前状态

- Android 原生项目
- Kotlin 2.0.21
- Jetpack Compose
- Material 3
- Java 17
- Android Gradle Plugin 8.7.3
- compileSdk 35
- minSdk 26
- targetSdk 35
- namespace：`com.blog.app`
- applicationId：`com.blog.app`

## 项目结构

```text
app/src/main/java/com/blog/app/
├── MainActivity.kt
├── core/
│   └── config/
├── data/
│   ├── api/
│   ├── model/
│   └── repository/
├── navigation/
└── ui/
    └── home/
```

## 后续规划

1. 登录与注册
2. OAuth2 / OIDC 登录
3. 博客首页
4. 文章列表与详情
5. 分类、标签与搜索
6. 评论
7. 用户中心
8. 与博客现有后端 API 对接
9. 网络层、Token、缓存和异常处理

## 开发说明

项目代码以便于长期维护为目标进行组织。类、方法和模块保持职责清晰，按业务模块逐步拆分。

Kotlin 不支持使用 `#` 作为注释语法，因此 Kotlin 方法内部注释使用 `//`；类、方法等声明外部使用 `/** */` 文档注释。
