plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

// 允许通过 -Pconnectivity.useSimulated=true 切换模拟器，默认启用真实网关
val useSimulatedProvisionerInDebug =
    (project.findProperty("connectivity.useSimulated") as? String)?.toBoolean() ?: false

// 支持通过 -Pconnectivity.bleProfilesJson 或环境变量覆盖 BLE Profile
val bleProfileOverridesRaw =
    (project.findProperty("connectivity.bleProfilesJson") as? String)
        ?: System.getenv("SMARTSALES_BLE_PROFILES_JSON")

fun String.toBuildConfigLiteral(): String =
    "\"" + this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n") + "\""

val bleProfileOverridesLiteral = (bleProfileOverridesRaw ?: "").toBuildConfigLiteral()

android {
    namespace = "com.smartsales.feature.connectivity"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField(
                "boolean",
                "USE_SIMULATED_PROVISIONER",
                useSimulatedProvisionerInDebug.toString()
            )
            buildConfigField(
                "String",
                "BLE_PROFILE_OVERRIDES",
                bleProfileOverridesLiteral
            )
        }
        release {
            buildConfigField("boolean", "USE_SIMULATED_PROVISIONER", "false")
            buildConfigField(
                "String",
                "BLE_PROFILE_OVERRIDES",
                bleProfileOverridesLiteral
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(projects.core.util)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.hilt.android)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)

    kapt(libs.hilt.compiler)

    testImplementation(projects.core.test)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso)
    debugImplementation(libs.compose.ui.test.manifest)
}
