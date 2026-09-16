# Android Home Articles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Android App 首页接入博客文章分类与分页接口，并提供文章列表和 Markdown 详情展示。

**Architecture:** 使用 Retrofit + OkHttp 负责 HTTP API，Repository 隔离数据访问，ViewModel 管理首页分页状态，Compose 负责列表与详情 UI。文章列表只展示元数据/摘要，点击后进入独立详情页并渲染 Markdown；接口地址统一从 `ApiConfig` 获取。

**Tech Stack:** Kotlin, Android, Jetpack Compose, Material3, Retrofit, OkHttp, Kotlinx Serialization, Markdown 渲染库, Java 17, minSdk 26, targetSdk 35.

**Spec:** `docs/superpowers/specs/2026-09-16-android-app-foundation-design.md`

## Global Constraints

- Package namespace and applicationId remain `com.blog.app`.
- Network base URL is `http://124.221.195.130/api/content/`.
- Article list must support `pageNum` and `pageSize` pagination.
- Initial article query uses `type=0`, `selectUser=0`, `selectStatus=1,2`, `sortType=0,1`.
- Article body is Markdown and must be rendered as Markdown in the detail screen.
- Code comments inside methods use `//`; comments outside methods/classes use `/** */`.
- Commit messages are Chinese.

---

### Task 1: Network dependencies and API configuration

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/blog/app/core/config/ApiConfig.kt`
- Create: `app/src/main/java/com/blog/app/core/network/NetworkModule.kt`

**Interfaces:**
- Produces a configured Retrofit service factory using `ApiConfig.BASE_URL`.
- `ApiConfig.BASE_URL` must equal `http://124.221.195.130/api/content/`.

- [ ] **Step 1: Add Retrofit, OkHttp, serialization dependencies and Kotlin serialization plugin.**

- [ ] **Step 2: Configure Android cleartext HTTP access because the current backend is HTTP.**

- [ ] **Step 3: Create a reusable Retrofit instance with JSON serialization and OkHttp timeouts.**

- [ ] **Step 4: Verify Gradle dependency resolution and Kotlin compilation.**

- [ ] **Step 5: Commit with `新增：配置文章接口网络层`.**

---

### Task 2: Article API models and service

**Files:**
- Create: `app/src/main/java/com/blog/app/data/model/article/Article.kt`
- Create: `app/src/main/java/com/blog/app/data/model/article/ArticlePage.kt`
- Create: `app/src/main/java/com/blog/app/data/model/article/ArticleType.kt`
- Create: `app/src/main/java/com/blog/app/data/api/ArticleApi.kt`

**Interfaces:**
- `ArticleApi.getArticleTypes(): ...`
- `ArticleApi.getArticles(pageNum: Int, pageSize: Int, type: Long, selectUser: Int, selectStatus: String, sortType: String): ...`

- [ ] **Step 1: Define serializable models matching the backend JSON envelope and preserve unknown fields safely.**

- [ ] **Step 2: Define the article list endpoint with the exact query parameters supplied by the existing website.**

- [ ] **Step 3: Define the category tree endpoint.**

- [ ] **Step 4: Create an API factory from `NetworkModule`.**

- [ ] **Step 5: Compile the data layer.**

- [ ] **Step 6: Commit with `新增：文章接口与数据模型`.**

---

### Task 3: Repository and pagination state

**Files:**
- Create: `app/src/main/java/com/blog/app/data/repository/ArticleRepository.kt`
- Create: `app/src/main/java/com/blog/app/ui/home/HomeViewModel.kt`

**Interfaces:**
- Repository exposes article page loading and category loading without leaking Retrofit details to UI.
- ViewModel exposes immutable Compose state containing articles, loading state, refreshing state, error message, current page, and whether more data exists.

- [ ] **Step 1: Implement repository methods that call the API with the initial website query parameters.**

- [ ] **Step 2: Implement first-page loading and append-next-page behavior.**

- [ ] **Step 3: Stop requesting additional pages when the server indicates no more records or the returned page is smaller than `pageSize`.**

- [ ] **Step 4: Add retry behavior that does not duplicate already-loaded articles.**

- [ ] **Step 5: Compile the ViewModel and repository.**

- [ ] **Step 6: Commit with `新增：首页文章分页状态管理`.**

---

### Task 4: Home article list UI

**Files:**
- Modify: `app/src/main/java/com/blog/app/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/blog/app/navigation/AppNavigation.kt`

**Interfaces:**
- Home screen consumes `HomeViewModel` state and emits article-click events.

- [ ] **Step 1: Replace the temporary initialization screen with a blog article feed.**

- [ ] **Step 2: Display article title, category/date metadata, and a concise preview without rendering the full Markdown body in every list item.**

- [ ] **Step 3: Add loading, empty, error, retry, and footer-loading states.**

- [ ] **Step 4: Trigger the next page when the user reaches the end of the list.**

- [ ] **Step 5: Wire article selection into navigation.**

- [ ] **Step 6: Compile and run the Compose UI tests/build.**

- [ ] **Step 7: Commit with `新增：首页文章列表与分页加载`.**

---

### Task 5: Markdown article detail

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/blog/app/ui/article/ArticleDetailScreen.kt`
- Modify: `app/src/main/java/com/blog/app/navigation/AppNavigation.kt`

**Interfaces:**
- Detail screen accepts an `Article` and renders its Markdown body.

- [ ] **Step 1: Add a maintained Android Markdown rendering dependency compatible with the current Compose/AGP toolchain.**

- [ ] **Step 2: Render headings, paragraphs, emphasis, lists, code blocks, links, images, and tables where supported by the selected renderer.**

- [ ] **Step 3: Add title and basic metadata above the Markdown body.**

- [ ] **Step 4: Handle missing/empty Markdown content gracefully.**

- [ ] **Step 5: Compile the detail screen and verify navigation.**

- [ ] **Step 6: Commit with `新增：文章Markdown详情页`.**

---

### Task 6: End-to-end verification

**Files:**
- No new production files unless verification exposes a concrete defect.

- [ ] **Step 1: Run the complete Gradle build using the repository wrapper and verify Gradle 8.9 is used.**

- [ ] **Step 2: Verify the app can request `/article/type/tree`.**

- [ ] **Step 3: Verify the app can request page 1 and page 2 of `/article/list` with the required query parameters.**

- [ ] **Step 4: Verify pagination appends rather than replaces page 1 data.**

- [ ] **Step 5: Verify article detail opens and Markdown is rendered.**

- [ ] **Step 6: Commit any verification-only fixes with a Chinese commit message.**
