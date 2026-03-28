package com.smartsales.prism.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val PrismTopSafeBand = 16.dp

@Composable
fun Modifier.prismStatusBarPadding(): Modifier = this
    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))

@Composable
fun Modifier.prismStatusBarTopSafeBandPadding(): Modifier = this
    .prismStatusBarPadding()
    .padding(top = PrismTopSafeBand)

@Composable
fun Modifier.prismNavigationBarPadding(): Modifier = this
    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))

@Composable
fun Modifier.prismMonolithTopInsetPadding(monolithOffset: Dp): Modifier = this
    .prismStatusBarPadding()
    .padding(top = monolithOffset)

@Composable
fun Modifier.prismTopSafeBandPadding(): Modifier = this
    .prismStatusBarTopSafeBandPadding()
