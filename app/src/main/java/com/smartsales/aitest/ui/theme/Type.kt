package com.smartsales.aitest.ui.theme

// 文件：app/src/main/java/com/smartsales/aitest/ui/theme/Type.kt
// 模块：:app
// 说明：Compose 字体体系 — "Sleek Glass" (Pro Max) 排版
//       使用 Inter/SF Pro 风格, -0.02em 紧凑字间距
// 作者：重写于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 回退至系统无衬线字体 (理想情况下加载 Inter 或 SF Pro)
val Inter = FontFamily.SansSerif

/** Pro Max 紧凑字间距 (-0.02em ≈ -0.32sp for 16sp base) */
private val ProMaxLetterSpacing = (-0.32).sp

val MaterialTypography = Typography(
    // ================================================================
    // Display (Hero/Brand)
    // ================================================================
    displayLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = ProMaxLetterSpacing
    ),
    displayMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = ProMaxLetterSpacing
    ),
    displaySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = ProMaxLetterSpacing
    ),

    // ================================================================
    // Headline (Section Titles)
    // ================================================================
    headlineLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = ProMaxLetterSpacing
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = ProMaxLetterSpacing
    ),
    headlineSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = ProMaxLetterSpacing
    ),

    // ================================================================
    // Title (App Bar, Card Titles)
    // ================================================================
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = ProMaxLetterSpacing
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = ProMaxLetterSpacing
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = ProMaxLetterSpacing
    ),

    // ================================================================
    // Body (Content Text)
    // ================================================================
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp // 正文使用标准间距
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),

    // ================================================================
    // Label (Buttons, Chips, Captions)
    // ================================================================
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    )
)
