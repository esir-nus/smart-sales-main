package com.smartsales.prism.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ShellLayoutModeTest {

    @Test
    fun `height thresholds map to tall compact and tight modes`() {
        assertEquals(ShellLayoutMode.TIGHT, resolveShellLayoutMode(400.dp, 699.dp))
        assertEquals(ShellLayoutMode.COMPACT, resolveShellLayoutMode(400.dp, 700.dp))
        assertEquals(ShellLayoutMode.COMPACT, resolveShellLayoutMode(400.dp, 819.dp))
        assertEquals(ShellLayoutMode.TALL, resolveShellLayoutMode(400.dp, 820.dp))
    }

    @Test
    fun `narrow width clamps one mode tighter`() {
        assertEquals(ShellLayoutMode.COMPACT, resolveShellLayoutMode(379.dp, 820.dp))
        assertEquals(ShellLayoutMode.TIGHT, resolveShellLayoutMode(379.dp, 700.dp))
        assertEquals(ShellLayoutMode.TIGHT, resolveShellLayoutMode(379.dp, 699.dp))
    }
}
