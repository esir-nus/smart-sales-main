package com.smartsales.prism.ui.sim

/**
 * 跟进改期 V2 影子实验的进程内计数器。
 * 说明：只用于实验观测，不参与任何业务决策。
 */
internal object SimFollowUpRescheduleShadowMetrics {

    data class Snapshot(
        val started: Int = 0,
        val parity: Int = 0,
        val mismatchTime: Int = 0,
        val mismatchSupport: Int = 0,
        val invalid: Int = 0,
        val failure: Int = 0
    )

    private var snapshot = Snapshot()

    @Synchronized
    fun onStarted(): Snapshot {
        snapshot = snapshot.copy(started = snapshot.started + 1)
        return snapshot
    }

    @Synchronized
    fun onParity(): Snapshot {
        snapshot = snapshot.copy(parity = snapshot.parity + 1)
        return snapshot
    }

    @Synchronized
    fun onMismatchTime(): Snapshot {
        snapshot = snapshot.copy(mismatchTime = snapshot.mismatchTime + 1)
        return snapshot
    }

    @Synchronized
    fun onMismatchSupport(): Snapshot {
        snapshot = snapshot.copy(mismatchSupport = snapshot.mismatchSupport + 1)
        return snapshot
    }

    @Synchronized
    fun onInvalid(): Snapshot {
        snapshot = snapshot.copy(invalid = snapshot.invalid + 1)
        return snapshot
    }

    @Synchronized
    fun onFailure(): Snapshot {
        snapshot = snapshot.copy(failure = snapshot.failure + 1)
        return snapshot
    }

    @Synchronized
    fun current(): Snapshot = snapshot

    @Synchronized
    fun resetForTest() {
        snapshot = Snapshot()
    }
}
