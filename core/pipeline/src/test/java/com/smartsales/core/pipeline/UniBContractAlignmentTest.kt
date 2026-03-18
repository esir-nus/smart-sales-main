package com.smartsales.core.pipeline

import com.smartsales.prism.domain.scheduler.UniBExtractionPayload
import com.smartsales.prism.domain.scheduler.UniBExtractionRequest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import org.junit.Assert.assertTrue
import org.junit.Test

class UniBContractAlignmentTest {

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Uni-B prompt must explicitly contain all fields expected by the linter contract`() {
        val compiler = PromptCompiler()
        val prompt = compiler.compileUniBExtractionPrompt(
            UniBExtractionRequest(
                transcript = "三天以后提醒我开会",
                nowIso = "2026-03-18T10:00:00Z",
                timezone = "Asia/Shanghai",
                unifiedId = "uni-b-001",
                displayedDateIso = "2026-03-19"
            )
        )

        val descriptor = UniBExtractionPayload.serializer().descriptor
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
            "Uni-B prompt/linter drift: missing keys $missingKeys",
            missingKeys.isEmpty()
        )
        assertTrue(prompt.contains("displayed_date_iso"))
        assertTrue(prompt.contains("明天"))
        assertTrue(prompt.contains("下一天"))
        assertTrue(prompt.contains("13:00"))
        assertTrue(prompt.contains("01:00"))
        assertTrue(prompt.contains("不要编造今天"))
        assertTrue(prompt.contains("明天提醒我打电话"))
        assertTrue(prompt.contains("不得补出 `00:00`"))
    }
}
