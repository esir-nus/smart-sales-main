package com.smartsales.aitest.ui.theme

// 文件：app/src/main/java/com/smartsales/aitest/ui/theme/Shape.kt
// 模块：:app
// 说明：Compose 圆角体系 — "Sleek Glass" (Pro Max) 圆角
//       现代连续曲线: 20dp (来自 index.css --radius-card: 20px)
// 作者：重写于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Pro Max 风格圆角体系
 * - extraSmall: 4dp (微控件)
 * - small: 8dp (按钮、芯片)
 * - medium: 12dp (输入框)
 * - large: 20dp (卡片、模态) ← Pro Max 标准
 * - extraLarge: 28dp (底部弹窗)
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp), // Pro Max 卡片圆角
    extraLarge = RoundedCornerShape(28.dp)
)

// ================================================================
// 常用快捷形状
// ================================================================
/** 玻璃卡片圆角 - 20dp (Pro Max 标准) */
val GlassCardShape = RoundedCornerShape(20.dp)

/** 按钮圆角 - 12dp */
val ButtonShape = RoundedCornerShape(12.dp)

/** 全圆角 (药丸形) */
val PillShape = RoundedCornerShape(percent = 50)
