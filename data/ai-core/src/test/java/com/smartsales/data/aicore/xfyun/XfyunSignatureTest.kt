// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/xfyun/XfyunSignatureTest.kt
// 模块：:data:ai-core
// 说明：验证讯飞签名 baseString 构造与 HMAC 输出稳定性
// 作者：创建于 2025-12-15
package com.smartsales.data.aicore.xfyun

import org.junit.Assert.assertEquals
import org.junit.Test

class XfyunSignatureTest {

    @Test
    fun `sign builds deterministic baseString and signature`() {
        val signature = XfyunSignature(accessKeySecret = "testsecret")
        val params = mapOf(
            "accessKeyId" to "testaccesskeyid",
            "dateTime" to "2018-04-13T20:22:53+0800",
            "fileName" to "demo audio+.mp3",
            "fileSize" to "397144",
            "signatureRandom" to "a0289d60-26b3",
        )

        val baseString = signature.buildBaseString(
            queryParams = params,
            encodeKeys = false
        )

        assertEquals(
            "accessKeyId=testaccesskeyid&dateTime=2018-04-13T20%3A22%3A53%2B0800&fileName=demo+audio%2B.mp3&fileSize=397144&signatureRandom=a0289d60-26b3",
            baseString
        )

        val signed = signature.sign(
            queryParams = params,
            encodeKeys = false
        )
        assertEquals("YknSc7FLNkMupEkv5lC9xlxnvGQ=", signed)
    }
}

