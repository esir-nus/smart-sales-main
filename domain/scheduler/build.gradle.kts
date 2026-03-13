plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.smartsales.domain.scheduler"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation("javax.inject:javax.inject:1")
    implementation(project(":core:util"))
    
    // Dependencies used by the Linter/Repository
    implementation(project(":domain:core"))
    implementation(project(":domain:memory"))
    
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation("org.json:json:20231013")
    testImplementation(project(":core:test-fakes"))
}
