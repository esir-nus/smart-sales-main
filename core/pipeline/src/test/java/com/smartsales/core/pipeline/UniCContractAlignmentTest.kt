package com.smartsales.core.pipeline

import com.smartsales.prism.domain.scheduler.UniCExtractionPayload
import com.smartsales.prism.domain.scheduler.UniCExtractionRequest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import org.junit.Assert.assertTrue
import org.junit.Test

class UniCContractAlignmentTest {

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Uni-C prompt must explicitly contain all fields expected by the linter contract`() {
        val compiler = PromptCompiler()
        val prompt = compiler.compileUniCExtractionPrompt(
            UniCExtractionRequest(
                transcript = "以后想练口语",
                nowIso = "2026-03-18T10:00:00Z",
                timezone = "Asia/Shanghai",
                unifiedId = "uni-c-001"
            )
        )

        val descriptor = UniCExtractionPayload.serializer().descriptor
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
            "Uni-C prompt/linter drift: missing keys $missingKeys",
            missingKeys.isEmpty()
        )
        assertTrue(prompt.contains("INSPIRATION_CREATE"))
        assertTrue(prompt.contains("NOT_INSPIRATION"))
        assertTrue(prompt.contains("明天提醒我打电话"))
    }
}
