package com.smartsales.core.pipeline

import com.smartsales.prism.domain.scheduler.FollowUpRescheduleExtractionPayload
import com.smartsales.prism.domain.scheduler.FollowUpRescheduleExtractionRequest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowUpRescheduleContractAlignmentTest {

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `follow-up reschedule V2 prompt must explicitly contain all fields expected by the linter contract`() {
        val compiler = PromptCompiler()
        val prompt = compiler.compileFollowUpRescheduleExtractionPrompt(
            FollowUpRescheduleExtractionRequest(
                transcript = "改到明天早上8点",
                nowIso = "2026-03-22T08:00:00Z",
                timezone = "Asia/Shanghai",
                selectedTaskStartIso = "2026-03-22T08:00:00Z",
                selectedTaskDurationMinutes = 30,
                selectedTaskTitle = "赶高铁",
                selectedTaskLocation = "虹桥站",
                selectedTaskPerson = "张总"
            )
        )

        val descriptor = FollowUpRescheduleExtractionPayload.serializer().descriptor
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
            "Follow-up reschedule V2 prompt/linter drift: missing keys $missingKeys",
            missingKeys.isEmpty()
        )
        assertTrue(prompt.contains("selected_task_title"))
        assertTrue(prompt.contains("selected_task_start_iso"))
        assertTrue(prompt.contains("推迟1小时"))
        assertTrue(prompt.contains("提前半小时"))
        assertTrue(prompt.contains("明天早上8点"))
        assertTrue(prompt.contains("RELATIVE_DAY_CLOCK"))
        assertTrue(prompt.contains("DELTA_FROM_TARGET"))
        assertTrue(prompt.contains("NOT_SUPPORTED"))
        assertTrue(prompt.contains("页面相对日期"))
    }
}
