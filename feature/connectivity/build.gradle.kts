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
    kapt(libs.hilt.compiler)

    testImplementation(projects.core.test)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
