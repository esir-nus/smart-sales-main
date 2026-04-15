plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    
    configurations.all {
        resolutionStrategy.eachDependency {
            // 固定动态注解版本，避免第三方 SDK 的 latest.release 触发远端元数据查询。
            if (requested.group == "org.jetbrains" &&
                requested.name == "annotations" &&
                requested.version == "latest.release"
            ) {
                useVersion("26.1.0")
                because("Keep Android builds installable when remote Maven metadata is unavailable")
            }
        }
        resolutionStrategy.capabilitiesResolution
            .withCapability("com.google.guava:listenablefuture") {
                select("com.google.guava:listenablefuture:1.0")
            }
    }
}
