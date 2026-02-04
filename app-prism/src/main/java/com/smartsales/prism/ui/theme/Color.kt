package com.smartsales.prism.ui.theme

import androidx.compose.ui.graphics.Color

// ===================================================================
// Pro Max Gray Background
// ===================================================================
/** 应用主背景 - Pro Max Gray #F5F5F7 */
val BackgroundApp = Color(0xFFF5F5F7)

/** 卡片/表面 - 高透明度白色 (70% 不透明度) */
val BackgroundSurface = Color(0xB3FFFFFF) // White @ 70%

/** 表面 Muted - 极淡黑色 (2% 不透明度) */
val BackgroundSurfaceMuted = Color(0x05000000) // Black @ 2%

/** 表面 Hover 态 */
val BackgroundSurfaceHover = Color(0xFFFAFAFA)

/** 表面 Active 态 */
val BackgroundSurfaceActive = Color(0xFFE5E5EA)

// ===================================================================
// Typography Colors (SF 风格)
// ===================================================================
/** 主要文本 - SF Black #1D1D1F */
val TextPrimary = Color(0xFF1D1D1F)

/** 次要文本 - SF Gray #86868B */
val TextSecondary = Color(0xFF86868B)

/** 弱化文本 / Muted */
val TextMuted = Color(0xFF8E8E93)

/** 三级文本 */
val TextTertiary = Color(0xFFC7C7CC)

/** 强调文本 */
val TextStrong = Color(0xFF3A3A3C)

// ===================================================================
// Borders
// ===================================================================
/** 边框 - 超细 Ultra-fine (4% 不透明度) */
val BorderSubtle = Color(0x0A000000) // Black @ 4%

/** 边框宽度 - 0.5dp (Retina hairline) - 在 Theme 模块中定义为 Dp */

// ===================================================================
// Accents
// ===================================================================
/** 主要强调色 - Stark Black (Pro Max 风格) */
val AccentPrimary = Color(0xFF000000)

/** 深层主要强调色 */
val AccentPrimaryDeep = Color(0xFF1D1D1F)

/** 次要强调色 - Green/Teal */
val AccentSecondary = Color(0xFF34C759)
val AccentGreen = AccentSecondary // Alias

/** 三级强调色 - Purple */
val AccentTertiary = Color(0xFF5856D6)

/** 四级强调色 - Yellow (Graph/Charts) */
val AccentYellow = Color(0xFFFFCC00)

/** 默认蓝色 (用于链接、按钮等 - iOS Blue) */
val AccentBlue = Color(0xFF007AFF)

/** 琥珀色 (用于改期高亮) */
val AccentAmber = Color(0xFFFFA726)

/** 危险/警告色 */
val AccentDanger = Color(0xFFEF4444)
val AccentRed = AccentDanger // Alias

/** 危险表面色 */
val SurfaceDanger = Color(0xFFFEF2F2)

// ===================================================================
// Shadows & Overlays
// ===================================================================
/** 玻璃阴影 - Pro Max Deep Shadow */
// 定义为: 0 20px 40px -10px rgba(0, 0, 0, 0.05)
// Compose 中通过 elevation + shadowColor 实现

/** 幕布遮罩 */
val BackdropScrim = Color(0x4D000000) // Black @ 30%

// ===================================================================
// Semantic Colors (Knot 系统 - 来自 Prism Spec)
// ===================================================================
/** Knot 主色 - Teal/Cyan */
val KnotPrimary = Color(0xFF14B8A6)

/** Knot 次色 */
val KnotSecondary = Color(0xFF2DD4BF)
