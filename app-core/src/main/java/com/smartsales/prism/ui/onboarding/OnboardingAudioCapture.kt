package com.smartsales.prism.ui.onboarding

import android.content.Context
import com.smartsales.prism.data.audio.PhoneAudioRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * onboarding 手机端录音接口。
 */
interface OnboardingAudioCapture {
    fun startRecording()
    fun stopRecording(): File
    fun cancelRecording()
    fun isRecording(): Boolean
}

/**
 * 真实 onboarding 手机录音实现。
 */
@Singleton
class RealOnboardingAudioCapture @Inject constructor(
    @ApplicationContext context: Context
) : OnboardingAudioCapture {

    private val recorder = PhoneAudioRecorder(context)

    override fun startRecording() {
        recorder.startRecording()
    }

    override fun stopRecording(): File = recorder.stopRecording()

    override fun cancelRecording() {
        if (recorder.isRecording()) {
            recorder.cancel()
        }
    }

    override fun isRecording(): Boolean = recorder.isRecording()
}
