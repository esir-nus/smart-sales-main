// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/xfyun/XfyunAsrApiUploadParamsTest.kt
// 模块：:data:ai-core
// 说明：锁定 /v2/upload 参数构造规则（包含 eng_smoothproc 且参与签名）
// 作者：创建于 2025-12-16
package com.smartsales.data.aicore.xfyun

import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.AiParaSettingsSnapshot
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class XfyunAsrApiUploadParamsTest {

    @Test
    fun buildUploadParams_respectsEngSmoothproc() {
        val temp = File.createTempFile("xfyun", ".wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            deleteOnExit()
        }
        val api = XfyunAsrApi(
            httpClient = XfyunHttpClient(),
            configProvider = XfyunConfigProvider(
                object : AiParaSettingsProvider {
                    override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
                }
            ),
            traceStore = XfyunTraceStore(),
        )

        val method = XfyunAsrApi::class.java.getDeclaredMethod(
            "buildUploadParams",
            XfyunCredentials::class.java,
            File::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            String::class.java,
            Long::class.javaObjectType,
            String::class.java,
            String::class.java,
            String::class.java,
            XfyunParamStrategy::class.java,
        ).apply {
            isAccessible = true
        }

        val credentials = XfyunCredentials(
            appId = "app",
            accessKeyId = "id",
            accessKeySecret = "secret",
            baseUrl = "https://example.com",
        )

        @Suppress("UNCHECKED_CAST")
        val params = method.invoke(
            api,
            credentials,
            temp,
            "autodialect",
            1,
            0,
            false,
            "transfer",
            null,
            "rand16",
            "2025-12-16T00:00:00+0800",
            "1234567890",
            XfyunParamStrategy.DOC_FIRST,
        ) as Map<String, String>

        assertEquals("false", params["eng_smoothproc"])
    }
}

