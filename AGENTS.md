# AGENTS.md

爱车保养 · Car Maintenance — 本地优先的 Android 汽车保养记录应用（Kotlin · Jetpack Compose · Room · WebDAV 同步）。无广告、无账号，数据存在本地 Room 数据库，可选开启 WebDAV 云端同步。

## 环境要求 · Prerequisites

- JDK 17（AGP 8.7.3 / Gradle 8.9 要求；`sourceCompatibility` 与 `jvmTarget` 均为 17）
- Android SDK，compileSdk 35 / minSdk 26 / targetSdk 35
- 未配置 `local.properties` 时，需设置环境变量 `ANDROID_HOME`（该文件不入库，已在 .gitignore）

## 常用命令 · Commands

- 编译调试包：`./gradlew assembleDebug`（Windows：`gradlew.bat assembleDebug`）
- 编译发布包：`./gradlew assembleRelease`
- 安装到已连接设备：`./gradlew installDebug`
- 静态检查：`./gradlew lint`
- 单元测试：`./gradlew test`（当前仓库暂无测试源集）
- 清理构建产物：`./gradlew clean`

## 项目结构 · Project layout

- `app/src/main/java/com/carmaintenance/app/data/` — Room 数据层
  - `entity/` — 实体：Vehicle / MaintenanceRecord / PartItem / PartHistory / LocationHistory
  - `dao/` — DAO 接口，查询返回 `Flow`
  - `repository/` — 仓库层，对 ViewModel 暴露 Flow 与 suspend 方法
  - `AppDatabase.kt` — 数据库单例（`getInstance`）；`DbMutex.kt` — 全局写锁
- `app/src/main/java/com/carmaintenance/app/ui/` — Compose UI 层
  - `screen/` — 页面：Home / RecordEdit / VehicleEdit / VehicleManager / Settings
  - `viewmodel/` — ViewModel，状态用 StateFlow
  - `theme/` — Material 3 主题；`AppNavigation.kt`、`MainActivity.kt` — 导航与入口
- `app/src/main/java/com/carmaintenance/app/util/` — 工具：WebDavManager（同步）、CsvExporter（导出）、Formatters、SyncStatusBus
- `app/src/main/res/` — 资源：图标、`values/strings.xml`（中文文案）、主题、备份规则
- `docs/screenshots/` — README 配图

## 架构约定 · Architecture

- MVVM + Repository + Flow：UI → ViewModel → Repository → DAO，单向依赖，不要跨层直连
- DAO 暴露 `Flow`，UI 用 `collectAsState()` 订阅；一次性动作（导入、同步、删除）用 `suspend`
- 无 DI 框架，依赖手动构造：`AppDatabase.getInstance(context)` 单例 + 手动 new Repository / ViewModel
- IO 与数据库写入必须离开主线程（`withContext(Dispatchers.IO)`）；会改库的操作走 `DbMutex.mutex.withLock` 串行化，避免自动同步清库与用户保存互相踩踏
- Compose 状态提升到 ViewModel，Composable 只做渲染与事件回调

## 代码风格 · Code style

- Kotlin 官方风格（`gradle.properties` 中 `kotlin.code.style=official`），4 空格缩进
- 注释与界面文案用中文；标识符、类名、方法名仍用英文
- 主题色一律取自 `MaterialTheme.colorScheme`，不要在 Composable 里硬编码颜色
- 日志用 `android.util.Log` + 私有 `TAG` 常量
- Room 实体或表结构变动时必须提升 `AppDatabase` 的 `version`；当前使用 `fallbackToDestructiveMigration()`，未写 Migration 会直接清空用户数据
- 新增依赖加在 `app/build.gradle.kts`，版本号与现有条目风格保持一致

## 测试 · Testing

- 目前没有 `app/src/test` 与 `app/src/androidTest`，改动行为时应补测试
- 纯逻辑（Formatters、CsvExporter、费用汇总）放 JVM 单元测试；DAO / 数据库相关用 `androidx.room:room-testing` 配仪器测试（`app/src/androidTest`）
- 新增依赖时记得补 `testImplementation` / `androidTestImplementation`，目前仅配置了 runner

## 构建与依赖 · Build & deps

- Gradle Kotlin DSL；AGP 8.7.3 / Kotlin 1.9.24 / KSP 1.9.24-1.0.20 / Compose BOM 2024.10.01
- `settings.gradle.kts` 已配置阿里云镜像加速（放在官方源之前，官方源保留为 fallback）；要直连官方源时删掉或注释镜像 maven 块
- Gradle wrapper 走 `services.gradle.org`，国内下载慢时把 `gradle/wrapper/gradle-wrapper.properties` 的 `distributionUrl` 换成镜像地址
- 发布包未开启混淆（`isMinifyEnabled = false`），调整后再考虑补 `proguard-rules.pro` 规则

## 提交规范 · Commit

- 默认分支 `main`，单人维护：改动直接在 `main` 提交，不另开特性分支
- Commit message 用约定式前缀：`feat:` / `fix:` / `docs:` / `refactor:` / `chore:`
- 提交前至少跑通 `./gradlew assembleDebug`，影响数据层时额外跑 `lint`

## 安全 · Security

- 绝不提交密钥与本地配置：`*.jks`、`*.keystore`、`local.properties` 已由 .gitignore 覆盖
- WebDAV 账号密码只存本地 SharedPreferences（`carmaintenance`），不要写进源码、示例数据或日志；同步失败日志不得输出凭据
- 导出的 CSV 只在用户主动触发时生成到用户可见位置，不要默认落到外部存储公共目录
