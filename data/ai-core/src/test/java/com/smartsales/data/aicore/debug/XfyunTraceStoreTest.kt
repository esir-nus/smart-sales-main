// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/debug/XfyunTraceStoreTest.kt
// 模块：:data:ai-core
// 说明：验证 XFyun 调试痕迹的脱敏、截断与复制格式稳定性
// 作者：创建于 2025-12-15
package com.smartsales.data.aicore.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XfyunTraceStoreTest {

    @Test
    fun `recordUploadAttempt redacts accessKeyId and removes signature`() {
        val store = XfyunTraceStore()
        store.recordUploadAttempt(
            baseUrl = "https://example.com",
            uploadParams = linkedMapOf(
                "appId" to "appid",
                "accessKeyId" to "abcdefghijklmnop",
                "signature" to "should-not-store",
                "accessKeySecret" to "should-not-store",
                "fileName" to "demo.wav",
                "resultType" to "transfer",
                "eng_smoothproc" to "true",
            ),
            roleType = 1,
            roleNum = 0,
        )

        val snapshot = store.getSnapshot()
        assertNotNull(snapshot)
        val params = snapshot!!.uploadParams
        assertEquals("https://example.com", snapshot.baseUrl)
        assertEquals("abcd…mnop", params["accessKeyId"])
        assertFalse(params.containsKey("signature"))
        assertFalse(params.containsKey("accessKeySecret"))
        assertEquals("transfer", params["resultType"])
        assertEquals("true", params["eng_smoothproc"])
        assertTrue(snapshot.resultTypeAttempts.any { it.phase == "upload" && it.resultType == "transfer" })
    }

    @Test
    fun `recordFailure truncates and scrubs payload`() {
        val store = XfyunTraceStore()
        val secret = "my-secret-value"
        val payload = "{\"accessKeySecret\":\"$secret\",\"data\":\"${"a".repeat(10_000)}\"}"
        store.recordFailure(payloadSnippet = payload)

        val snippet = store.getSnapshot()?.rawPayloadSnippet
        assertNotNull(snippet)
        assertTrue(snippet!!.length <= 8_000)
        assertFalse(snippet.contains(secret))
        assertTrue(snippet.contains("\"accessKeySecret\":\"<redacted>\""))
    }

    @Test
    fun `debug formatter output stays readable and redacted`() {
        val snapshot = XfyunTraceSnapshot(
            baseUrl = "https://example.com",
            uploadParams = mapOf(
                "accessKeyId" to "abcd…wxyz",
                "fileName" to "demo.wav",
            ),
            resultTypeAttempts = listOf(
                XfyunTraceSnapshot.ResultTypeAttempt(
                    tsMs = 1L,
                    phase = "getResult",
                    resultType = "transfer",
                    downgradedBecauseFailType11 = false,
                )
            ),
            orderId = "order-123",
            resultType = "transfer,predict",
            roleType = 1,
            roleNum = 0,
            pollTimeline = listOf(
                XfyunTraceSnapshot.PollEntry(
                    tsMs = 1L,
                    status = 3,
                    failType = 0,
                    httpCode = 200,
                )
            ),
            pollCount = 1,
            elapsedMs = 5_000L,
            lastHttpCode = 200,
            lastFailType = 11,
            lastFailDesc = "not enabled",
            rawPayloadSnippet = "{\"code\":\"000000\"}",
            updatedAtMs = 2L,
        )

        val text = XfyunDebugInfoFormatter.format(snapshot)
        assertTrue(text.contains("\"provider\": \"XFyun\""))
        assertTrue(text.contains("\"baseUrl\": \"https://example.com\""))
        assertTrue(text.contains("\"orderId\": \"order-123\""))
        assertTrue(text.contains("\"downgradedBecauseFailType11\": false"))
        assertTrue(text.contains("\"resultTypeAttempts\":"))
        assertFalse(text.contains("accessKeySecret"))
        assertFalse(text.contains("\"signature\""))
    }

    @Test
    fun `recordResultTypeAttempt sets downgrade flag`() {
        val store = XfyunTraceStore()
        store.recordUploadAttempt(
            baseUrl = "https://example.com",
            uploadParams = mapOf("resultType" to "transfer"),
            roleType = 1,
            roleNum = 0,
        )
        store.recordResultTypeAttempt(
            phase = "getResult",
            resultType = "transfer",
            downgradedBecauseFailType11 = true,
        )

        val snapshot = store.getSnapshot()
        assertNotNull(snapshot)
        assertTrue(snapshot!!.downgradedBecauseFailType11)
        assertTrue(snapshot.resultTypeAttempts.any { it.downgradedBecauseFailType11 })
    }
}
