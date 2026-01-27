package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.entities.ArtifactMeta

/**
 * 文件引用仓库
 */
interface ArtifactRepository {
    suspend fun getById(id: String): ArtifactMeta?
    suspend fun getByEntryId(entryId: String): List<ArtifactMeta>
    suspend fun insert(artifact: ArtifactMeta)
    suspend fun delete(id: String)
}
