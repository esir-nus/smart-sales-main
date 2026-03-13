package com.smartsales.core.pipeline

import com.smartsales.prism.domain.core.UnifiedMutation
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.Assert.assertTrue
import org.junit.Test

class TempSchemaTest {
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `schema valid`() {
        val descriptor = Json.serializersModule.serializer<UnifiedMutation>().descriptor
        val hasRecommendedWorkflows = (0 until descriptor.elementsCount).any { 
            descriptor.getElementName(it) == "recommended_workflows"
        }
        assertTrue("UnifiedMutation is missing recommended_workflows", hasRecommendedWorkflows)
        println("SUCCESS: unified_mutation_schema_valid")
    }
}
