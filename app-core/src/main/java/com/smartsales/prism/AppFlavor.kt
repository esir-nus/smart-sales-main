package com.smartsales.prism

internal object AppFlavor {
    val isHarmonyCompat: Boolean
        get() = BuildConfig.IS_HARMONY_COMPAT_FLAVOR

    val schedulerEnabled: Boolean
        get() = BuildConfig.ENABLE_SCHEDULER
}
