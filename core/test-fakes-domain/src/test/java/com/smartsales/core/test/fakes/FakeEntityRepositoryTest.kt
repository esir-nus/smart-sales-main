package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeEntityRepositoryTest {

    @Test
    fun `save supports read through alias lookup and delete`() = runTest {
        val repository = FakeEntityRepository()
        val entry = EntityEntry(
            entityId = "contact-1",
            entityType = EntityType.CONTACT,
            displayName = "Frank Zhang",
            aliasesJson = """["Frank","Zhang总"]""",
            lastUpdatedAt = 20L,
            createdAt = 10L
        )

        repository.save(entry)

        assertEquals(entry, repository.getById("contact-1"))
        assertEquals(1, repository.getByIdCount)
        assertEquals(listOf(entry), repository.findByAlias("Frank"))

        repository.delete("contact-1")

        assertNull(repository.getById("contact-1"))
        assertEquals(2, repository.getByIdCount)
    }
}
