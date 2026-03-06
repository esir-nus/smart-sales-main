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
    // SessionContext depends on EntityWriter (crm), RLModule (habit) and MemoryCenter (memory)?
    implementation(project(":domain:crm"))
    implementation(project(":domain:habit"))
    implementation(project(":domain:memory"))
}
