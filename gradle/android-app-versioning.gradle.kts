import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun Project.runVersioningCommand(vararg command: String): String? {
    return runCatching {
        val output = providers.exec {
            commandLine(*command)
        }.standardOutput.asText.get().trim()
        output.takeIf { it.isNotBlank() }
    }.getOrNull()
}

fun Project.resolveAndroidAppVersion(baseVersionName: String, baseVersionCode: Int): Map<String, Any> {
    val buildStampOverride = providers.gradleProperty("SMARTSALES_BUILD_STAMP").orNull
        ?: providers.environmentVariable("SMARTSALES_BUILD_STAMP").orNull
    val buildStamp = buildStampOverride?.trim().takeUnless { it.isNullOrBlank() }
        ?: ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
            .format(DateTimeFormatter.ofPattern("yyDDD.HHmm"))

    val gitSha = runVersioningCommand("git", "rev-parse", "--short=7", "HEAD")
        ?.replace(Regex("[^0-9A-Za-z]"), "")
        ?.lowercase()
        .takeUnless { it.isNullOrBlank() }
        ?: "nogit"

    val debugVersionSuffix = "-debug.$buildStamp.$gitSha"

    return mapOf(
        "baseVersionCode" to baseVersionCode,
        "baseVersionName" to baseVersionName,
        "buildStamp" to buildStamp,
        "gitSha" to gitSha,
        "debugVersionSuffix" to debugVersionSuffix,
        "debugDisplayVersion" to "$baseVersionName$debugVersionSuffix"
    )
}

extra["resolveAndroidAppVersion"] = { baseVersionName: String, baseVersionCode: Int ->
    resolveAndroidAppVersion(baseVersionName, baseVersionCode)
}
