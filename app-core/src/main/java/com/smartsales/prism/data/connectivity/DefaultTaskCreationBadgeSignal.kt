package com.smartsales.prism.data.connectivity

import com.smartsales.core.pipeline.TaskCreationBadgeSignal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTaskCreationBadgeSignal @Inject constructor() : TaskCreationBadgeSignal {
    override suspend fun onTasksCreated() {
        // Command#end 仅属于录音流水线终态；任务创建成功不再触发 BLE 信号。
        Unit
    }
}
