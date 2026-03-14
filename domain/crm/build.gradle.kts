plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":domain:habit"))
    api(project(":domain:memory"))
    api(project(":domain:scheduler"))
    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
