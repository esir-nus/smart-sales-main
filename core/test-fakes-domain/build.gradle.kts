plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Core Domain Contracts
    implementation(project(":domain:core"))
    implementation(project(":domain:crm"))
    implementation(project(":domain:memory"))
    implementation(project(":domain:habit"))
    implementation(project(":domain:session"))
    implementation(project(":domain:scheduler"))
    
    // Kotlin & Coroutines
    implementation(libs.coroutines.core)

    // Javax Inject annotations
    implementation("javax.inject:javax.inject:1")

    // JSON parsing for fakes
    implementation(libs.kotlinx.serialization.json)
    implementation("org.json:json:20231013")
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
