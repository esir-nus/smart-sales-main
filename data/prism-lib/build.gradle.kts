plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.smartsales.data.prismlib"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        
        // Room Schema export location
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas"
                )
            }
        }
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
    implementation(projects.domain.prismCore)
    implementation(projects.core.util)
    
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    
    // Network & AI
    implementation(libs.dashscope.sdk)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.aliyun.tingwu.sdk) {
         exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
         exclude(group = "org.bouncycastle", module = "bcprov-ext-jdk15on")
    }
    
    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    
    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
