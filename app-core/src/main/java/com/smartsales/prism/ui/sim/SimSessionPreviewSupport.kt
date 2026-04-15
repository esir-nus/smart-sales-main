package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.model.SessionKind
import com.smartsales.prism.domain.model.SessionPreview

internal val SessionPreview.hasAudioContextHistory: Boolean
    get() = linkedAudioId != null || sessionKind == SessionKind.AUDIO_GROUNDED
