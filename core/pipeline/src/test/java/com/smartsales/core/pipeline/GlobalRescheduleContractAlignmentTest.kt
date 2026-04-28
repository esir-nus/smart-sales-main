package com.smartsales.core.pipeline

import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionPayload
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionRequest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalRescheduleContractAlignmentTest {

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `global reschedule prompt must describe explicit delta support`() {
        val compiler = PromptCompiler()
        val prompt = compiler.compileGlobalRescheduleExtractionPrompt(
            GlobalRescheduleExtractionRequest(
                transcript = "把拿合同推迟1个小时",
                nowIso = "2026-03-22T08:00:00Z",
                timezone = "Asia/Shanghai"
            )
        )

        val descriptor = GlobalRescheduleExtractionPayload.serializer().descriptor
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
            "Global reschedule prompt/linter drift: missing keys $missingKeys",
            missingKeys.isEmpty()
        )
        assertTrue(prompt.contains("推迟1个小时"))
        assertTrue(prompt.contains("提前半小时"))
        assertTrue(prompt.contains("newTitle"))
        assertTrue(prompt.contains("改成9点赶飞机"))
        assertTrue(prompt.contains("9点的任务"))
        assertTrue(prompt.contains("晚上8点的开会取消了"))
        assertTrue(prompt.contains("取消晚上8点的开会"))
        assertTrue(prompt.contains("去机场接人"))
        assertTrue(prompt.contains("RESCHEDULE_TARGETED"))
        assertTrue(prompt.contains("待会儿"))
    }
}
