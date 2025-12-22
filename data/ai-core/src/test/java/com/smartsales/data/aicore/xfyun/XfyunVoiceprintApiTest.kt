// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/xfyun/XfyunVoiceprintApiTest.kt
// 模块：:data:ai-core
// 说明：验证声纹注册 UI 包装方法的输入校验（不触发真实网络）
// 作者：创建于 2025-12-19
package com.smartsales.data.aicore.xfyun

import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.AiParaSettingsSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Test

class XfyunVoiceprintApiTest {

    @Test
    fun `registerBase64 rejects blank base64`() = runBlocking {
        val api = XfyunVoiceprintApi(
            httpClient = XfyunHttpClient(),
            configProvider = XfyunConfigProvider(
                object : AiParaSettingsProvider {
                    override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
                }
            ),
        )
        try {
            api.registerBase64(audioDataBase64 = "   ", audioType = "raw", uid = null)
            fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // ok
        }
    }

    @Test
    fun `registerBase64 rejects blank audioType`() = runBlocking {
        val api = XfyunVoiceprintApi(
            httpClient = XfyunHttpClient(),
            configProvider = XfyunConfigProvider(
                object : AiParaSettingsProvider {
                    override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
                }
            ),
        )
        try {
            api.registerBase64(audioDataBase64 = "dGVzdA==", audioType = "   ", uid = null)
            fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // ok
        }
    }
}

