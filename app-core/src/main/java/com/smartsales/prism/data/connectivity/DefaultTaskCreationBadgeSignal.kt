package com.smartsales.prism.data.connectivity

import com.smartsales.core.pipeline.TaskCreationBadgeSignal
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTaskCreationBadgeSignal @Inject constructor(
    private val deviceConnectionManager: DeviceConnectionManager
) : TaskCreationBadgeSignal {
    override suspend fun onTasksCreated() {
        deviceConnectionManager.notifyTaskCreated()
    }
}
