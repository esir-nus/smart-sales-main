plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":domain:habit"))
    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
