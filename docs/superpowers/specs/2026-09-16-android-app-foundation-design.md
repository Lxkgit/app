# Android App Foundation Design

## Goal

将博客 Android App 重构为便于长期维护的 Kotlin + Jetpack Compose 工程，并统一使用 `com.blog.app` 包名。

## Architecture

采用按职责拆分的基础结构：`core` 负责通用能力，`data` 负责数据访问与模型，`navigation` 负责页面导航，`ui` 按业务页面组织。第一阶段只实现可运行的首页骨架，不提前引入登录、文章等业务实现。

## Package and Directory

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

Android `namespace` 和 `applicationId` 均为 `com.blog.app`。

## Build Stack

- Android
- Kotlin 2.0.21
- Jetpack Compose
- Material 3
- Java 17
- compileSdk 35
- targetSdk 35
- minSdk 26
- Android Gradle Plugin 8.7.3

## Coding Rules

- Kotlin 方法内部注释使用 `//`。
- 类、方法、属性等声明外部的说明使用 `/** */`。
- 不使用 Kotlin 不支持的 `#` 注释。
- 第一阶段不引入无实际用途的第三方库。
