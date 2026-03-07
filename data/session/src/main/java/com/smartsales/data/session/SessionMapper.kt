package com.smartsales.data.session

import com.smartsales.prism.data.persistence.SessionEntity
import com.smartsales.prism.domain.model.SessionPreview

fun SessionEntity.toDomain(): SessionPreview = SessionPreview(
    id = sessionId,
    clientName = clientName,
    summary = summary,
    timestamp = timestamp,
    isPinned = isPinned,
    linkedAudioId = linkedAudioId
)

fun SessionPreview.toEntity(): SessionEntity = SessionEntity(
    sessionId = id,
    clientName = clientName,
    summary = summary,
    timestamp = timestamp,
    isPinned = isPinned,
    linkedAudioId = linkedAudioId
)
