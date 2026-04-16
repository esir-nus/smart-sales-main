package com.smartsales.prism.ui.sim

// hasAudioContextHistory 现在是 SessionPreview 的存储字段，不再需要扩展属性。
// 创建音频会话时必须显式设置 hasAudioContextHistory = true。
// 从磁盘反序列化时 SimSessionRepository.toDomain() 也会正确计算该字段。
