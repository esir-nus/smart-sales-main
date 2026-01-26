package com.smartsales.data.prismlib.repositories

import com.smartsales.domain.prism.core.entities.ArtifactMeta
import com.smartsales.domain.prism.core.repositories.ArtifactRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件系统 Artifact 仓库
 * Phase 2: 简化实现 — 存储在内存映射中 (正式版本需 Room 表)
 */
@Singleton
class FileSystemArtifactRepository @Inject constructor() : ArtifactRepository {

    // In-memory store for Phase 2 (simplest implementation)
    private val artifacts = mutableMapOf<String, ArtifactMeta>()
    private val byEntryId = mutableMapOf<String, MutableList<ArtifactMeta>>()

    override suspend fun getById(id: String): ArtifactMeta? {
        return artifacts[id]
    }

    override suspend fun getByEntryId(entryId: String): List<ArtifactMeta> {
        return byEntryId[entryId] ?: emptyList()
    }

    override suspend fun insert(artifact: ArtifactMeta) {
        artifacts[artifact.artifactId] = artifact
        // Also index by entry if we had that info — for now just store
        // In real impl, entryId would come from context or artifact meta
    }

    override suspend fun delete(id: String) {
        val removed = artifacts.remove(id)
        // Clean up from byEntryId if exists
        byEntryId.values.forEach { it.removeAll { a -> a.artifactId == id } }
    }
}
