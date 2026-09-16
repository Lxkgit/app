# Android App Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将博客 Android App 重构为 `com.blog.app` 包结构，并建立可长期扩展的 Compose 首页基础工程。

**Architecture:** 使用 `core`、`data`、`navigation`、`ui` 四个职责区域。首个可运行版本只包含应用入口、导航入口和首页，网络配置集中在 `core/config`，不提前绑定具体后端接口。

**Tech Stack:** Android, Kotlin 2.0.21, Jetpack Compose, Material 3, Java 17, AGP 8.7.3, compileSdk 35, minSdk 26。

**Spec:** `docs/superpowers/specs/2026-09-16-android-app-foundation-design.md`

## Global Constraints

- Kotlin 包名统一为 `com.blog.app`。
- `namespace` 和 `applicationId` 使用 `com.blog.app`。
- Kotlin 方法内部注释使用 `//`；声明外部使用 `/** */`。
- 不引入没有实际用途的第三方库。
- 保留 Android 互联网访问权限。

---

### Task 1: Update Android build configuration

**Files:**
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: Android application configuration using `com.blog.app`.

- [ ] **Step 1: Set AGP and Kotlin plugin versions**

Set the root plugins to AGP 8.7.3, Kotlin 2.0.21, and the matching Compose compiler plugin.

- [ ] **Step 2: Change Android namespace and application ID**

Set both values to `com.blog.app` while retaining SDK 35 and Java 17.

- [ ] **Step 3: Keep the initial dependency set minimal**

Retain Compose, Material 3, core-ktx, and activity-compose without adding networking libraries before the API design is finalized.

- [ ] **Step 4: Verify configuration consistency**

Confirm no remaining `com.lxkgit.blogapp` package appears in Gradle configuration.

- [ ] **Step 5: Commit**

Commit with `build: align Android project versions and package`.

### Task 2: Move application source to com.blog.app

**Files:**
- Create: `app/src/main/java/com/blog/app/MainActivity.kt`
- Create: `app/src/main/java/com/blog/app/core/config/ApiConfig.kt`
- Create: `app/src/main/java/com/blog/app/navigation/AppNavigation.kt`
- Create: `app/src/main/java/com/blog/app/ui/home/HomeScreen.kt`
- Delete: `app/src/main/java/com/lxkgit/blogapp/MainActivity.kt`
- Delete: `app/src/main/java/com/lxkgit/blogapp/core/ApiConfig.kt`

**Interfaces:**
- `BlogApp()` remains the application Compose entry.
- `HomeScreen()` is the first destination.
- `ApiConfig.BASE_URL` remains the centralized API configuration value.

- [ ] **Step 1: Create the new package files**

Use `package com.blog.app` and the corresponding subpackages. Keep `MainActivity` responsible only for launching Compose.

- [ ] **Step 2: Extract navigation into its own package**

Create an application navigation entry that currently resolves to the home screen without adding unnecessary navigation dependencies.

- [ ] **Step 3: Extract HomeScreen into ui/home**

Keep the initial screen simple and functional so it can be replaced incrementally when the real blog UI is designed.

- [ ] **Step 4: Move API configuration into core/config**

Keep the placeholder base URL centralized until the real API gateway address is confirmed.

- [ ] **Step 5: Remove the old package files**

Delete the old `com.lxkgit.blogapp` source files after the new files are in place.

- [ ] **Step 6: Commit**

Commit with `refactor: reorganize app package structure`.

### Task 3: Update documentation and verify project structure

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Document the new package structure**

Document `com.blog.app`, the Android/Compose stack, and the comment convention.

- [ ] **Step 2: Check source references**

Confirm the manifest activity `.MainActivity` still resolves through the new namespace and that no old package references remain.

- [ ] **Step 3: Verify Gradle sync/build**

In Android Studio, sync the project with the upgraded Android Studio/AGP combination and run the `app` configuration.

- [ ] **Step 4: Commit**

Commit with `docs: update Android app project structure`.
