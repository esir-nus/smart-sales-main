// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/params/XfyunResultSettingsTest.kt
// 模块：:data:ai-core
// 说明：锁定 resultType 默认值与能力护栏（避免误触发 failType=11）
// 作者：创建于 2025-12-16
package com.smartsales.data.aicore.params

import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.AiCoreException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class XfyunResultSettingsTest {

    @Test
    fun default_isTransfer() {
        val value = XfyunResultSettings().resolveApiValueOrThrow(XfyunCapabilities())
        assertEquals("transfer", value)
    }

    @Test
    fun translate_isBlockedWithoutCapability() {
        try {
            XfyunResultSettings(resultType = XfyunResultType.TRANSLATE)
                .resolveApiValueOrThrow(XfyunCapabilities())
            fail("expected AiCoreException")
        } catch (e: AiCoreException) {
            // 重要：能力未开通时必须在发请求前阻断，避免触发 failType=11。
            assertEquals(AiCoreErrorSource.XFYUN, e.source)
            assertEquals(AiCoreErrorReason.UNSUPPORTED_CAPABILITY, e.reason)
        }
    }
}

