package com.smartsales.prism.ui.sim

import androidx.compose.ui.unit.dp
import com.smartsales.prism.ui.components.ShellLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SimHomeHeroLayoutModeTest {

    @Test
    fun `normal phone widths stay in compact hero mode instead of tight top bias`() {
        assertEquals(
            ShellLayoutMode.COMPACT,
            SimHomeHeroTokens.resolveGreetingLayoutMode(
                availableWidth = 360.dp,
                availableHeight = 760.dp
            )
        )
        assertEquals(
            ShellLayoutMode.COMPACT,
            SimHomeHeroTokens.resolveGreetingLayoutMode(
                availableWidth = 360.dp,
                availableHeight = 700.dp
            )
        )
    }

    @Test
    fun `true constrained hero canvases still use tight mode`() {
        assertEquals(
            ShellLayoutMode.TIGHT,
            SimHomeHeroTokens.resolveGreetingLayoutMode(
                availableWidth = 360.dp,
                availableHeight = 619.dp
            )
        )
        assertEquals(
            ShellLayoutMode.TIGHT,
            SimHomeHeroTokens.resolveGreetingLayoutMode(
                availableWidth = 339.dp,
                availableHeight = 760.dp
            )
        )
    }
}
