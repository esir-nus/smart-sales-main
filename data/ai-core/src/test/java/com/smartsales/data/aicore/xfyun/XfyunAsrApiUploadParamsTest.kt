// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/xfyun/XfyunAsrApiUploadParamsTest.kt
// 模块：:data:ai-core
// 说明：锁定 /v2/upload 参数构造规则（包含 eng_smoothproc 且参与签名）
// 作者：创建于 2025-12-16
package com.smartsales.data.aicore.xfyun

import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.AiParaSettingsSnapshot
import com.smartsales.data.aicore.params.XfyunUploadSettings
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class XfyunAsrApiUploadParamsTest {

    @Test
    fun buildUploadParams_containsDocUploadParams() {
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
            aiParaSettingsProvider = object : AiParaSettingsProvider {
                override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
            }
        )

        val method = XfyunAsrApi::class.java.getDeclaredMethod(
            "buildUploadParams",
            XfyunCredentials::class.java,
            File::class.java,
            String::class.java,
            XfyunUploadSettings::class.java,
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

        val uploadSettings = XfyunUploadSettings(
            language = "autominor",
            pd = "finance",
            roleType = 1,
            roleNum = 2,
            engSmoothProc = true,
            engColloqProc = true,
            engVadMdn = 2,
            audioMode = "fileStream",
        )

        @Suppress("UNCHECKED_CAST")
        val params = method.invoke(
            api,
            credentials,
            temp,
            "autominor",
            uploadSettings,
            "transfer",
            null,
            "rand16",
            "2025-12-16T00:00:00+0800",
            "1234567890",
            XfyunParamStrategy.DOC_FIRST,
        ) as Map<String, String>

        // 锁定：与 docs/xfyun-asr-rest-api.md 的 upload 参数表一致
        assertEquals("autominor", params["language"])
        assertEquals("finance", params["pd"])
        assertEquals("1", params["roleType"])
        assertEquals("2", params["roleNum"])
        assertEquals("fileStream", params["audioMode"])
        assertEquals("true", params["eng_smoothproc"])
        assertEquals("true", params["eng_colloqproc"])
        assertEquals("2", params["eng_vad_mdn"])
        assertEquals("transfer", params["resultType"])
    }

    @Test
    fun buildUploadParams_omitsBlankPd() {
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
            aiParaSettingsProvider = object : AiParaSettingsProvider {
                override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
            }
        )

        val method = XfyunAsrApi::class.java.getDeclaredMethod(
            "buildUploadParams",
            XfyunCredentials::class.java,
            File::class.java,
            String::class.java,
            XfyunUploadSettings::class.java,
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

        val uploadSettings = XfyunUploadSettings(pd = "   ")

        @Suppress("UNCHECKED_CAST")
        val params = method.invoke(
            api,
            credentials,
            temp,
            "autodialect",
            uploadSettings,
            "transfer",
            null,
            "rand16",
            "2025-12-16T00:00:00+0800",
            "1234567890",
            XfyunParamStrategy.DOC_FIRST,
        ) as Map<String, String>

        // 重要：空 pd 不能出现在最终 URL 中，否则会导致签名/URL 不一致。
        assertFalse(params.containsKey("pd"))
    }
}
