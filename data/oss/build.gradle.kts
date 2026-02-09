import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

fun loadLocalProperties(): Properties {
    val props = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { props.load(it) }
    }
    return props
}

fun String.escapeForBuildConfig(): String = this.replace("\\", "\\\\").replace("\"", "\\\"")

val localProperties = loadLocalProperties()
val ossAccessKeyId = localProperties.getProperty("OSS_ACCESS_KEY_ID", "")
val ossAccessKeySecret = localProperties.getProperty("OSS_ACCESS_KEY_SECRET", "")
val ossBucketName = localProperties.getProperty("OSS_BUCKET_NAME", "")
val ossEndpoint = localProperties.getProperty("OSS_ENDPOINT", "https://oss-cn-beijing.aliyuncs.com")

android {
    namespace = "com.smartsales.data.oss"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "OSS_ACCESS_KEY_ID", "\"${ossAccessKeyId.escapeForBuildConfig()}\"")
        buildConfigField("String", "OSS_ACCESS_KEY_SECRET", "\"${ossAccessKeySecret.escapeForBuildConfig()}\"")
        buildConfigField("String", "OSS_BUCKET_NAME", "\"${ossBucketName.escapeForBuildConfig()}\"")
        buildConfigField("String", "OSS_ENDPOINT", "\"${ossEndpoint.escapeForBuildConfig()}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "project.properties"
            excludes += "core.properties"
        }
    }

    lint {
        disable += setOf(
            "GradleDependency",
            "TrustAllX509TrustManager" // 第三方 Aliyun SDK 内部实现
        )
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(17)
}

configurations.all {
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
}

dependencies {
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Aliyun OSS
    implementation(libs.aliyun.oss.sdk) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcprov-ext-jdk15on")
    }

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
