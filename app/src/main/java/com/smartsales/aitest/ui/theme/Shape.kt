package com.smartsales.aitest.ui.theme

// 文件：app/src/main/java/com/smartsales/aitest/ui/theme/Shape.kt
// 模块：:app
// 说明：Compose 圆角体系，来源于 globals.css 的 radius 变量
// 作者：创建于 2025-12-02

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)
