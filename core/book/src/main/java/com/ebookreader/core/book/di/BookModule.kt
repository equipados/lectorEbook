package com.ebookreader.core.book.di

import android.content.Context
import com.ebookreader.core.book.scanner.BookScanner
import com.ebookreader.core.book.scanner.BookScannerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.readium.r2.streamer.Streamer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BookBindsModule {

    @Binds
    @Singleton
    abstract fun bindBookScanner(impl: BookScannerImpl): BookScanner
}

@Module
@InstallIn(SingletonComponent::class)
object BookProvidesModule {

    @Provides
    @Singleton
    fun provideStreamer(
        @ApplicationContext context: Context
    ): Streamer = Streamer(context)
}
