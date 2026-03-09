plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // alias(libs.plugins.kotlin.kapt)
    // alias(libs.plugins.hilt)
}

android {
    namespace = "com.smartsales.core.pipeline"
    compileSdk = 34

    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core:util"))
    implementation(project(":core:telemetry"))
    
    // Kernel Dependencies
    api(project(":core:context"))
    api(project(":core:llm"))
    
    implementation(project(":data:ai-core"))
    
    api(project(":domain:crm"))
    api(project(":domain:memory"))
    api(project(":domain:session"))
    api(project(":domain:scheduler"))

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    
    // Core annotations
    implementation("javax.inject:javax.inject:1")
    
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(project(":core:test-fakes"))
    testImplementation("org.json:json:20231013")
}
