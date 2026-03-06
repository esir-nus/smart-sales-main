plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    
    // MemoryCenter interfaces might need to depend on nothing, or maybe CRM?
    // Wait, interface map says MemoryCenter (LTM) doesn't read from any other domains.
}
