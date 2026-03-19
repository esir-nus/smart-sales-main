package com.smartsales.core.pipeline

import com.smartsales.prism.domain.scheduler.UniAExtractionPayload
import com.smartsales.prism.domain.scheduler.UniAExtractionRequest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import org.junit.Assert.assertTrue
import org.junit.Test

class UniAContractAlignmentTest {

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Uni-A prompt must explicitly contain all fields expected by the linter contract`() {
        val compiler = PromptCompiler()
        val prompt = compiler.compileUniAExtractionPrompt(
            UniAExtractionRequest(
                transcript = "明天上午十点开会",
                nowIso = "2026-03-17T10:00:00Z",
                timezone = "Asia/Shanghai",
                unifiedId = "uni-a-001"
            )
        )

        val descriptor = UniAExtractionPayload.serializer().descriptor
        val requiredKeys = mutableSetOf<String>()

        fun extractKeys(desc: SerialDescriptor) {
            for (i in 0 until desc.elementsCount) {
                requiredKeys.add(desc.getElementName(i))
                val childDesc = desc.getElementDescriptor(i)
                if (childDesc.kind is StructureKind.CLASS) {
                    extractKeys(childDesc)
                } else if (childDesc.kind is StructureKind.LIST) {
                    val listElementDesc = childDesc.getElementDescriptor(0)
                    if (listElementDesc.kind is StructureKind.CLASS) {
                        extractKeys(listElementDesc)
                    }
                }
            }
        }

        extractKeys(descriptor)
        val missingKeys = requiredKeys.filter { !prompt.contains("\"$it\"") }

        assertTrue(
            "Uni-A prompt/linter drift: missing keys $missingKeys",
            missingKeys.isEmpty()
        )
        assertTrue(prompt.contains("displayed_date_iso"))
        assertTrue(prompt.contains("明天"))
        assertTrue(prompt.contains("后天"))
        assertTrue(prompt.contains("下一天"))
        assertTrue(prompt.contains("真实日期"))
        assertTrue(prompt.contains("后天晚上九点去接张总"))
        assertTrue(prompt.contains("13:00"))
        assertTrue(prompt.contains("01:00"))
        assertTrue(prompt.contains("明天提醒我打电话"))
        assertTrue(prompt.contains("不得擅自补成 `00:00`"))
    }
}
