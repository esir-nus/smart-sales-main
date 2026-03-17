package com.smartsales.core.pipeline

import org.junit.Test
import com.smartsales.prism.domain.core.UnifiedMutation
import java.io.File

class JsonSchemaGeneratorTest {
    @Test
    fun dumpSchema() {
        val schema = JsonSchemaGenerator.generateSchema(UnifiedMutation.serializer().descriptor, "  ")
        File("/tmp/json_schema_dump.json").writeText(schema)
    }
}
