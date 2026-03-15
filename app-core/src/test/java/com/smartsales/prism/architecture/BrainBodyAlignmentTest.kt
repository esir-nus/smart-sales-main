package com.smartsales.prism.architecture

import com.smartsales.core.pipeline.PromptCompiler
import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.context.ModeMetadata
import com.smartsales.prism.domain.model.Mode
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Mechanical check to enforce the "Brain-Body Contract".
 * This prevents the LLM Prompt (Brain) from drifting away from the Kotlin parsing logic (Body).
 */
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import com.smartsales.prism.domain.core.UnifiedMutation
class BrainBodyAlignmentTest {

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Brain prompt must explicitly contain all fields expected by the Body Linter`() {
        val compiler = PromptCompiler()
        val dummyContext = EnhancedContext(
            userText = "test setup",
            modeMetadata = ModeMetadata(currentMode = Mode.ANALYST)
        )
        
        val systemPrompt = compiler.compile(dummyContext)

        // Mechanically extract keys from UnifiedMutation and all its nested serializable subclasses
        val descriptor = UnifiedMutation.serializer().descriptor
        val requiredBodyKeys = mutableSetOf<String>()
        
        fun extractKeys(desc: SerialDescriptor) {
            for (i in 0 until desc.elementsCount) {
                // Ignore "@type" or polymorphic discriminator fields if present, focus on real properties
                requiredBodyKeys.add(desc.getElementName(i))
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

        // Check if the exact JSON key (with quotes) exists in the prompt schema definition
        val missingKeys = requiredBodyKeys.filter { !systemPrompt.contains("\"$it\"") }
        
        assertTrue(
            "CRITICAL ARCHITECTURE DRIFT: Brain-Body Contract Violation!\n" +
            "The PromptCompiler schema is missing JSON keys that the Kotlin Data Class requires: \$missingKeys\n" +
            "Update PromptCompiler.kt to include these fields in its JSON schema.",
            missingKeys.isEmpty()
        )
    }

    @Test
    fun `JsonSchemaGenerator must generate syntactically valid JSON`() {
        // Enforce that the generated schema envelope doesn't have hanging commas or unclosed brackets
        val schemaString = com.smartsales.core.pipeline.JsonSchemaGenerator.generateSchema(
            UnifiedMutation.serializer().descriptor, 
            "  "
        )
        try {
            org.json.JSONObject(schemaString)
        } catch (e: Exception) {
            org.junit.Assert.fail("CRITICAL ARCHITECTURE DRIFT: JsonSchemaGenerator produced invalid JSON syntax. \n\${schemaString}\nError: \${e.message}")
        }
    }

    @Test
    fun `Interface Map SOT must remain aligned with core architecture modules`() {
        // Run from app-core/ directory during Gradle test
        val possiblePaths = listOf(
            File("../docs/cerb/interface-map.md"),
            File("../../docs/cerb/interface-map.md"),
            File("docs/cerb/interface-map.md")
        )
        
        val mapFile = possiblePaths.firstOrNull { it.exists() }
        if (mapFile == null) {
            println("Skipping test, interface-map.md not found. (Run from project root)")
            return
        }
        
        val mapText = mapFile.readText()
        
        // Ensure the Markdown explicitly documents the existence of these critical components
        val coreArchitectureContracts = listOf(
            "EntityWriter",
            "UnifiedPipeline",
            "ContextBuilder",
            "Executor",
            "PluginRegistry"
        )
        
        val missing = coreArchitectureContracts.filter { !mapText.contains(it) }
        
        // Note: SchedulerLinter might not be in interface-map.md yet, this will fail and force doc update
        assertTrue(
            "DOCUMENTATION DRIFT: interface-map.md is missing documentation for core components: \$missing",
            missing.isEmpty()
        )
    }
}
