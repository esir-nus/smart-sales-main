package com.smartsales.prism.di

import com.smartsales.prism.data.audio.FunAsrRealtimeSpeechRecognizer
import com.smartsales.prism.data.audio.SimRealtimeSpeechRecognizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SimAudioChatModule {

    @Binds
    @Singleton
    abstract fun bindSimRealtimeSpeechRecognizer(
        impl: FunAsrRealtimeSpeechRecognizer
    ): SimRealtimeSpeechRecognizer
}
