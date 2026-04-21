package com.smartsales.core.pipeline

interface TaskCreationBadgeSignal {
    suspend fun onTasksCreated()

    data object NoOp : TaskCreationBadgeSignal {
        override suspend fun onTasksCreated() = Unit
    }
}
