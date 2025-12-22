// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/debug/DebugSnapshotRedactorTest.kt
// 模块：:data:ai-core
// 说明：验证 HUD 调试快照脱敏规则（密钥/签名不会泄露）
// 作者：创建于 2025-12-22
package com.smartsales.data.aicore.debug

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugSnapshotRedactorTest {
    @Test
    fun `redact masks secret patterns and x-amz params`() {
        val input = """
            apiKey=abc123
            Authorization: Bearer secret-token
            https://example.com/file?X-Amz-Signature=abc&X-Amz-Credential=def
            {"accessKeyId":"key","token":"value"}
        """.trimIndent()

        val redacted = DebugSnapshotRedactor.redact(input)
        assertFalse(redacted.contains("abc123"))
        assertFalse(redacted.contains("secret-token"))
        assertFalse(redacted.contains("X-Amz-Signature=abc"))
        assertFalse(redacted.contains("\"accessKeyId\":\"key\""))
        assertFalse(redacted.contains("\"token\":\"value\""))
        assertTrue(redacted.contains("<redacted>"))
    }
}
