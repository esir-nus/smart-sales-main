package com.smartsales.core.pipeline

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

/**
 * Dynamically converts a kotlinx.serialization SerialDescriptor into a readable JSON schema.
 * Replaces hardcoded {"field": "type"} strings in PromptCompiler to enforce the Brain/Body contract.
 */
@OptIn(ExperimentalSerializationApi::class)
object JsonSchemaGenerator {

    fun generateSchema(descriptor: SerialDescriptor, indent: String = ""): String {
        val sb = java.lang.StringBuilder()
        if (descriptor.kind is StructureKind.CLASS) {
            sb.appendLine("{")
            val newIndent = "$indent  "
            for (i in 0 until descriptor.elementsCount) {
                val name = descriptor.getElementName(i)
                val isOptional = descriptor.isElementOptional(i)
                val childDesc = descriptor.getElementDescriptor(i)
                
                sb.append("$newIndent\"$name\": ")
                
                // Add " (optional)" notation if it's optional
                val optionalMarker = if (isOptional) " (optional)" else ""
                
                when (childDesc.kind) {
                    is StructureKind.CLASS -> {
                        sb.append(generateSchema(childDesc, newIndent).trimEnd())
                    }
                    is StructureKind.LIST -> {
                        val listElementDesc = childDesc.getElementDescriptor(0)
                        sb.appendLine("[")
                        sb.append(newIndent + "  ")
                        sb.append(generateSchema(listElementDesc, newIndent + "  ").trimEnd())
                        sb.appendLine()
                        sb.append("$newIndent]")
                    }
                    else -> {
                        val typeName = childDesc.serialName.substringAfterLast('.')
                        sb.append("\"$typeName$optionalMarker\"")
                    }
                }
                
                if (i < descriptor.elementsCount - 1) {
                    sb.append(",")
                }
                sb.appendLine()
            }
            sb.append("$indent}")
        } else {
            sb.append("\"${descriptor.serialName.substringAfterLast('.')}\"")
        }
        return sb.toString()
    }
}
