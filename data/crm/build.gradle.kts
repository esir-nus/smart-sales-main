plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.smartsales.data.crm"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // Domain Models & Core Database
    implementation(project(":domain:crm"))
    implementation(project(":domain:memory"))
    implementation(project(":core:database"))
    implementation(project(":core:util"))

    // Other utilities
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    
    // Test
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(project(":core:test-fakes"))
}
