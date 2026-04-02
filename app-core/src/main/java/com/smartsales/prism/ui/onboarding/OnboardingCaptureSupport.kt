package com.smartsales.prism.ui.onboarding

internal fun consultationStateForCapture(
    captureState: OnboardingConsultationCaptureState
): OnboardingConsultationUiState = when (captureState) {
    OnboardingConsultationCaptureState.IDLE -> OnboardingConsultationUiState()
    OnboardingConsultationCaptureState.RECORDING -> OnboardingConsultationUiState(hasStartedInteracting = true, isRecording = true)
    OnboardingConsultationCaptureState.PROCESSING -> OnboardingConsultationUiState(
        hasStartedInteracting = true,
        isProcessing = true,
        processingPhase = OnboardingProcessingPhase.RECOGNIZING
    )
    OnboardingConsultationCaptureState.ONE_TURN_REVEALED -> OnboardingConsultationUiState(
        hasStartedInteracting = true,
        messages = listOf(
            OnboardingInteractionMessage(OnboardingMessageRole.USER, "帮我搞定这个客户"),
            OnboardingInteractionMessage(OnboardingMessageRole.AI, "好的，先告诉我这位客户的情况，遇到了什么卡点？")
        ),
        completedRounds = 1
    )
    OnboardingConsultationCaptureState.COMPLETE -> OnboardingConsultationUiState(
        hasStartedInteracting = true,
        messages = listOf(
            OnboardingInteractionMessage(OnboardingMessageRole.USER, "帮我搞定这个客户"),
            OnboardingInteractionMessage(OnboardingMessageRole.AI, "好的，先告诉我这位客户的情况，遇到了什么卡点？"),
            OnboardingInteractionMessage(OnboardingMessageRole.USER, "客户觉得我们的价格比竞品高，预算一直批不下来"),
            OnboardingInteractionMessage(OnboardingMessageRole.AI, "这个阶段可以尝试分享一些同行的成功案例作为切入点，证明长期 ROI。您做得很好！")
        ),
        completedRounds = 2
    )
    OnboardingConsultationCaptureState.ERROR -> OnboardingConsultationUiState(
        hasStartedInteracting = true,
        errorMessage = "网络连接波动，请重试"
    )
}

internal fun profileStateForCapture(
    captureState: OnboardingProfileCaptureState
): OnboardingProfileUiState = when (captureState) {
    OnboardingProfileCaptureState.IDLE -> OnboardingProfileUiState()
    OnboardingProfileCaptureState.RECORDING -> OnboardingProfileUiState(hasStartedInteracting = true, isRecording = true)
    OnboardingProfileCaptureState.PROCESSING -> OnboardingProfileUiState(
        hasStartedInteracting = true,
        isProcessing = true,
        processingPhase = OnboardingProcessingPhase.RECOGNIZING
    )
    OnboardingProfileCaptureState.EXTRACTED -> OnboardingProfileUiState(
        hasStartedInteracting = true,
        transcript = "我是王经理，做 SaaS 软件销售总监已经 8 年了。平时主要用微信和电话联系客户。",
        acknowledgement = "谢谢您的分享，我已经为您建立好了专属档案。",
        draft = OnboardingProfileDraft(
            displayName = "王经理",
            role = "销售总监",
            industry = "SaaS 软件服务",
            experienceYears = "8年",
            communicationPlatform = "微信 / 电话"
        )
    )
    OnboardingProfileCaptureState.ERROR -> OnboardingProfileUiState(
        hasStartedInteracting = true,
        errorMessage = "资料提取结果暂时不可用，请重试。"
    )
}

internal fun consultationProcessingLabel(phase: OnboardingProcessingPhase): String = when (phase) {
    OnboardingProcessingPhase.RECOGNIZING -> "正在识别语音..."
    OnboardingProcessingPhase.BUILDING_CONSULTATION_REPLY -> "正在整理建议..."
    else -> "正在整理建议..."
}

internal fun profileProcessingLabel(phase: OnboardingProcessingPhase): String = when (phase) {
    OnboardingProcessingPhase.RECOGNIZING -> "正在识别语音..."
    OnboardingProcessingPhase.BUILDING_PROFILE_RESULT -> "正在整理资料..."
    else -> "正在整理资料..."
}
