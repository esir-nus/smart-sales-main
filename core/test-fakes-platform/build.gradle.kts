plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.smartsales.core.test.fakes.platform"
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
    // Depend on domain fakes
    api(project(":core:test-fakes-domain"))

    // Core Infrastructure Contracts
    implementation(project(":core:llm"))
    implementation(project(":core:context"))
    implementation(project(":core:pipeline"))
    implementation(project(":core:util"))
    
    // Kotlin & Coroutines
    implementation(libs.coroutines.core)

    // Javax Inject
    implementation("javax.inject:javax.inject:1")
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
