package com.smartsales.prism.ui.onboarding

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color

internal val OnboardingBackground = Color(0xFF05060A)
internal val OnboardingCard = Color(0x14FFFFFF)
internal val OnboardingCardBorder = Color(0x26FFFFFF)
internal val OnboardingCardSoft = Color(0x12FFFFFF)
internal val OnboardingMuted = Color(0xFF9CA3AF)
internal val OnboardingText = Color(0xFFF3F7FF)
internal val OnboardingBlue = Color(0xFF38BDF8)
internal val OnboardingMint = Color(0xFF34D399)
internal val OnboardingAmber = Color(0xFFF59E0B)
internal val OnboardingField = Color(0x0DFFFFFF)
internal val OnboardingErrorSurface = Color(0x14F59E0B)
internal val OnboardingPrimarySurface = Color.White
internal val OnboardingPrimaryText = Color(0xFF05060A)
internal val OnboardingLogoTile = Color(0xFF12161E)

internal const val ONBOARDING_MIC_BUTTON_TEST_TAG = "onboarding_mic_button"
internal const val ONBOARDING_PERMISSIONS_CONTINUE_TEST_TAG = "onboarding_permissions_continue"
internal const val ONBOARDING_QUICK_START_CARD_TEST_TAG = "onboarding_quick_start_card"
internal const val ONBOARDING_QUICK_START_COUNT_BADGE_TEST_TAG = "onboarding_quick_start_count_badge"
internal const val ONBOARDING_QUICK_START_SUCCESS_NOTE_TEST_TAG = "onboarding_quick_start_success_note"
internal const val ONBOARDING_QUICK_START_ROW_TIME_TEST_TAG = "onboarding_quick_start_row_time"
internal const val ONBOARDING_QUICK_START_ROW_DESC_TEST_TAG = "onboarding_quick_start_row_desc"
internal const val ONBOARDING_QUICK_START_ROW_BELLS_TEST_TAG = "onboarding_quick_start_row_bells"
internal const val ONBOARDING_QUICK_START_ROW_DATE_TEST_TAG = "onboarding_quick_start_row_date"

internal tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
