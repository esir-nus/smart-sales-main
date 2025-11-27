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
val dashscopeApiKey = localProperties.getProperty("DASHSCOPE_API_KEY", "")
val dashscopeModel = localProperties.getProperty("DASHSCOPE_MODEL", "qwen-turbo")
val tingwuAppKey = localProperties.getProperty("TINGWU_APP_KEY", "")
val tingwuApiKey = localProperties.getProperty("TINGWU_API_KEY", tingwuAppKey)
val tingwuAccessKeyId = localProperties.getProperty("ALIBABA_CLOUD_ACCESS_KEY_ID", "")
val tingwuAccessKeySecret = localProperties.getProperty("ALIBABA_CLOUD_ACCESS_KEY_SECRET", "")
val tingwuSecurityToken = localProperties.getProperty("TINGWU_SECURITY_TOKEN", "")
val tingwuBaseUrl = localProperties.getProperty("TINGWU_BASE_URL")
    ?: error(
        """
        Missing TINGWU_BASE_URL in local.properties.
        Example: TINGWU_BASE_URL=https://tingwu.cn-beijing.aliyuncs.com/
        """.trimIndent()
    )
val tingwuModel = localProperties.getProperty("TINGWU_MODEL", "tingwu-service-insights")
val ossAccessKeyId = localProperties.getProperty("OSS_ACCESS_KEY_ID", "")
val ossAccessKeySecret = localProperties.getProperty("OSS_ACCESS_KEY_SECRET", "")
val ossBucketName = localProperties.getProperty("OSS_BUCKET_NAME", "")
val ossEndpoint = localProperties.getProperty("OSS_ENDPOINT", "https://oss-cn-beijing.aliyuncs.com")

android {
    namespace = "com.smartsales.data.aicore"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "DASHSCOPE_API_KEY", "\"${dashscopeApiKey.escapeForBuildConfig()}\"")
        buildConfigField("String", "DASHSCOPE_MODEL", "\"${dashscopeModel.escapeForBuildConfig()}\"")
        buildConfigField("String", "TINGWU_APP_KEY", "\"${tingwuAppKey.escapeForBuildConfig()}\"")
        buildConfigField("String", "TINGWU_API_KEY", "\"${tingwuApiKey.escapeForBuildConfig()}\"")
        buildConfigField(
            "String",
            "TINGWU_ACCESS_KEY_ID",
            "\"${tingwuAccessKeyId.escapeForBuildConfig()}\""
        )
        buildConfigField(
            "String",
            "TINGWU_ACCESS_KEY_SECRET",
            "\"${tingwuAccessKeySecret.escapeForBuildConfig()}\""
        )
        buildConfigField(
            "String",
            "TINGWU_SECURITY_TOKEN",
            "\"${tingwuSecurityToken.escapeForBuildConfig()}\""
        )
        buildConfigField("String", "TINGWU_BASE_URL", "\"${tingwuBaseUrl.escapeForBuildConfig()}\"")
        buildConfigField("String", "TINGWU_MODEL", "\"${tingwuModel.escapeForBuildConfig()}\"")
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
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/NOTICE.md"
        }
    }

    lint {
        disable += setOf(
            "GradleDependency", // compileSdk 提示暂不升级
            "TrustAllX509TrustManager" // 第三方 Aliyun SDK 内部实现
        )
    }
}

kotlin {
    jvmToolchain(17)
}

configurations.all {
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
}

dependencies {
    implementation(projects.core.util)
    implementation(libs.coroutines.core)
    implementation(libs.hilt.android)
    implementation(libs.dashscope.sdk)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.aliyun.oss.sdk) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcprov-ext-jdk15on")
    }
    implementation(libs.aliyun.java.sdk.core) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcprov-ext-jdk15on")
    }
    implementation(libs.aliyun.tingwu.sdk) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcprov-ext-jdk15on")
    }
    implementation(libs.bcprov)
    kapt(libs.hilt.compiler)

    testImplementation(projects.core.test)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(kotlin("test"))
}
