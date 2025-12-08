package com.smartsales.aitest.onboarding

// 文件：app/src/main/java/com/smartsales/aitest/onboarding/OnboardingGateViewModel.kt
// 模块：:app
// 说明：暴露引导完成状态供入口选择起始页面
// 作者：创建于 2025-12-10

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.feature.usercenter.data.OnboardingStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class OnboardingGateViewModel @Inject constructor(
    onboardingStateRepository: OnboardingStateRepository
) : ViewModel() {
    val completed: StateFlow<Boolean> = onboardingStateRepository.completedFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
