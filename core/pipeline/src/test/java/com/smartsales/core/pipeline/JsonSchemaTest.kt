package com.smartsales.core.pipeline

import org.junit.Test
import com.smartsales.prism.domain.core.UnifiedMutation

class JsonSchemaTest {
    @Test
    fun testPrint() {
        val schema = JsonSchemaGenerator.generateSchema(UnifiedMutation.serializer().descriptor, "  ")
        println(schema)
    }
}
