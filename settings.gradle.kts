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
includeModule(":tingwuTestApp")
includeModule(":data:ai-core")
includeModule(":core:util")
includeModule(":core:test")
includeModule(":feature:chat")
includeModule(":feature:media")
includeModule(":feature:connectivity")
