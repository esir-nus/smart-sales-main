// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/XfyunSignature.kt
// 模块：:data:ai-core
// 说明：实现讯飞 Ifasr_llm 的签名算法（HMAC-SHA1 + Base64）
// 作者：创建于 2025-12-15
package com.smartsales.data.aicore.xfyun

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class XfyunSignature(
    private val accessKeySecret: String,
) {

    /**
     * 生成 signature 请求头。
     *
     * @param encodeKeys 用于兼容文档/示例不一致：部分示例只编码 value，不编码 key。
     */
    fun sign(
        queryParams: Map<String, String>,
        encodeKeys: Boolean,
    ): String {
        val baseString = buildBaseString(
            queryParams = queryParams,
            encodeKeys = encodeKeys,
        )
        val mac = Mac.getInstance(HMAC_SHA1)
        mac.init(
            SecretKeySpec(
                accessKeySecret.toByteArray(StandardCharsets.UTF_8),
                HMAC_SHA1
            )
        )
        val signBytes = mac.doFinal(baseString.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(signBytes)
    }

    /**
     * 构造 baseString（用于签名），便于单测校验排序与编码行为。
     */
    internal fun buildBaseString(
        queryParams: Map<String, String>,
        encodeKeys: Boolean,
    ): String {
        val sorted = queryParams
            .filter { (key, value) ->
                key != SIGNATURE_KEY && value.isNotBlank()
            }
            .toSortedMap()
        return sorted.entries.joinToString("&") { (key, value) ->
            val encodedKey = if (encodeKeys) urlEncode(key) else key
            val encodedValue = urlEncode(value)
            "$encodedKey=$encodedValue"
        }
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        private const val HMAC_SHA1 = "HmacSHA1"
        private const val SIGNATURE_KEY = "signature"
    }
}

