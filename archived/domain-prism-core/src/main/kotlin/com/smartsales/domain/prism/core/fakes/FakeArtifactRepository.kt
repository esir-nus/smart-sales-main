package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.entities.ArtifactMeta
import com.smartsales.domain.prism.core.repositories.ArtifactRepository

class FakeArtifactRepository : ArtifactRepository {
    private val store = mutableMapOf<String, ArtifactMeta>()
    private val entryIndex = mutableMapOf<String, MutableList<String>>()
    
    override suspend fun getById(id: String) = store[id]
    
    override suspend fun getByEntryId(entryId: String) = 
        entryIndex[entryId]?.mapNotNull { store[it] } ?: emptyList()
    
    override suspend fun insert(artifact: ArtifactMeta) {
        store[artifact.artifactId] = artifact
    }
    
    override suspend fun delete(id: String) {
        store.remove(id)
    }
}
