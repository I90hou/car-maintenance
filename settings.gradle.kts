pluginManagement {
    repositories {
        // 阿里云镜像：本地默认开启（国内拉取快）。
        // CI 跑在境外 runner 上，maven.aliyun.com 会返回 502 导致插件解析失败，
        // 所以 CI 传 -PchinaMirror=false 走官方源。
        if ((gradle.startParameter.projectProperties["chinaMirror"] ?: "true").toBoolean()) {
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if ((gradle.startParameter.projectProperties["chinaMirror"] ?: "true").toBoolean()) {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "CarMaintenance"
include(":app")
