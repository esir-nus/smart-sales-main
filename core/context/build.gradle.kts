plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.smartsales.core.context"
    compileSdk = 34

    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core:util"))
    implementation(project(":core:telemetry"))
    
    // OS Layer access
    api(project(":domain:session"))
    api(project(":domain:memory"))
    api(project(":domain:habit"))
    api(project(":domain:crm"))
    api(project(":domain:scheduler"))
    
    // Utilities
    implementation(project(":core:database")) // If needed for legacy time provider
    
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
