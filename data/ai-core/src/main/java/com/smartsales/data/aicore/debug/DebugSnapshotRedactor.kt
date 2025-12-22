// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugSnapshotRedactor.kt
// 模块：:data:ai-core
// 说明：HUD 调试文本脱敏（确定性规则，避免泄露密钥/签名）
// 作者：创建于 2025-12-22
package com.smartsales.data.aicore.debug

object DebugSnapshotRedactor {
    // 重要：HUD 即使在 debug 也必须脱敏，防止签名/Token 等敏感信息泄露。
    fun redact(input: String): String {
        if (input.isBlank()) return input
        var result = input
        result = redactUrlQuery(result)
        result = redactHeaders(result)
        result = redactJsonKeys(result)
        result = redactKeyValuePairs(result)
        return result
    }

    private fun redactUrlQuery(text: String): String {
        var result = text
        // 重要：屏蔽 X-Amz-* 签名参数（包含 X-Amz-Signature/X-Amz-Credential 等）。
        result = result.replace(
            Regex("([?&])(X-Amz-[^=&]+)=([^&#\\s]+)", RegexOption.IGNORE_CASE),
            "$1$2=<redacted>"
        )
        return result
    }

    private fun redactHeaders(text: String): String {
        // 重要：屏蔽 Authorization/Cookie 头信息（避免 token 泄露）。
        return text.replace(
            Regex("(?i)(authorization|cookie)\\s*:\\s*[^\\r\\n]+"),
            "$1: <redacted>"
        )
    }

    private fun redactJsonKeys(text: String): String {
        var result = text
        SENSITIVE_KEYS.forEach { key ->
            result = result.replace(
                Regex("\"$key\"\\s*:\\s*\"[^\"]*\"", RegexOption.IGNORE_CASE),
                "\"$key\":\"<redacted>\""
            )
        }
        return result
    }

    private fun redactKeyValuePairs(text: String): String {
        var result = text
        SENSITIVE_KEYS.forEach { key ->
            val escaped = Regex.escape(key)
            result = result.replace(
                Regex("(?i)($escaped)\\s*=\\s*([^\\s,;]+)"),
                "$1=<redacted>"
            )
            result = result.replace(
                Regex("(?i)($escaped)\\s*:\\s*([^\\s,;]+)"),
                "$1:<redacted>"
            )
        }
        return result
    }

    private val SENSITIVE_KEYS = listOf(
        "apiKey",
        "api_key",
        "apikey",
        "secret",
        "token",
        "signature",
        "authorization",
        "cookie",
        "appSecret",
        "accessKey",
        "access_key",
        "accessKeyId",
        "access_key_id",
        "accessKeySecret",
        "access_key_secret",
        "sessionToken",
        "securityToken",
        "x-amz-algorithm",
        "x-amz-credential",
        "x-amz-date",
        "x-amz-expires",
        "x-amz-signedheaders",
        "x-amz-signature",
        "x-amz-security-token",
    )
}
