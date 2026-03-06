import org.gradle.api.initialization.resolve.RepositoriesMode

rootProject.name = "main_app"

pluginManagement {
    repositories {
        maven { url = uri("$rootDir/third_party/maven-repo") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
        maven { url = uri("$rootDir/third_party/maven-repo") }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

fun includeModule(path: String) {
    include(path)
}

includeModule(":app")
includeModule(":app-core")  // Core Application Shell (Orchestrator, Features, UI)
includeModule(":tingwuTestApp")
includeModule(":data:ai-core")
includeModule(":data:oss")
includeModule(":data:connectivity")
includeModule(":data:asr")
includeModule(":data:tingwu")
includeModule(":core:util")
includeModule(":core:telemetry")
includeModule(":core:notifications")
includeModule(":core:test")
// ARCHIVED: domain:prism-core, data:prism-lib, feature:* (see archived/ folder)

includeModule(":domain:crm")
includeModule(":domain:memory")
includeModule(":domain:habit")
includeModule(":domain:session")
