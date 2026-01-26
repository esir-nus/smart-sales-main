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
        resolutionStrategy.capabilitiesResolution
            .withCapability("com.google.guava:listenablefuture") {
                select("com.google.guava:listenablefuture:1.0")
            }
    }
}
