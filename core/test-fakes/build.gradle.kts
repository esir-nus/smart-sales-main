plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.smartsales.core.test.fakes"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Core Domain Contracts
    implementation(project(":domain:crm"))
    implementation(project(":domain:memory"))
    implementation(project(":domain:habit"))
    implementation(project(":domain:session"))
    implementation(project(":domain:scheduler"))
    
    // Core Infrastructure Contracts
    implementation(project(":core:llm"))
    implementation(project(":core:context"))
    implementation(project(":core:pipeline"))
    
    // Kotlin & Coroutines
    implementation(libs.coroutines.core)

    // Javax Inject (For Dagger/Hilt bindings if needed downstream)
    implementation("javax.inject:javax.inject:1")
    
    // JSON parsing for fakes
    implementation("org.json:json:20231013")
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
