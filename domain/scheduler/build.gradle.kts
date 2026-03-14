plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation("javax.inject:javax.inject:1")
    
    // Dependencies used by the Linter/Repository
    implementation(project(":domain:core"))
    implementation(project(":domain:memory"))
    
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation("org.json:json:20231013")
    testImplementation(project(":core:test-fakes"))
}
