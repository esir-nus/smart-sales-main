plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.smartsales.feature.media"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(projects.core.util)
    implementation(projects.feature.connectivity)
    implementation(libs.coroutines.core)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    testImplementation(projects.core.test)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
