package com.smartsales.prism.data.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class OemCompatTest {

    @Test
    fun `huawei auto start targets prefer harmony app launch control activity first`() {
        val targets = OemCompat.autoStartTargetsForManufacturer("huawei")

        assertEquals(
            listOf(
                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            ),
            targets.map { it.className }
        )
    }

    @Test
    fun `honor auto start targets follow the shared huawei family fallback order`() {
        val targets = OemCompat.autoStartTargetsForManufacturer("honor")

        assertEquals(
            listOf(
                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            ),
            targets.map { it.className }
        )
    }
}
