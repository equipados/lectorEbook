package com.ebookreader.core.tts.di

import com.ebookreader.core.tts.cache.TtsCacheManager
import com.ebookreader.core.tts.cache.TtsCacheManagerImpl
import com.ebookreader.core.tts.controller.TtsController
import com.ebookreader.core.tts.controller.TtsControllerImpl
import com.ebookreader.core.tts.engine.CloudTtsEngine
import com.ebookreader.core.tts.engine.TtsEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TtsModule {

    @Binds
    @Singleton
    abstract fun bindTtsController(impl: TtsControllerImpl): TtsController

    @Binds
    @Singleton
    abstract fun bindTtsCacheManager(impl: TtsCacheManagerImpl): TtsCacheManager

    @Binds
    @Singleton
    abstract fun bindCloudTtsEngine(impl: CloudTtsEngine): TtsEngine
}
