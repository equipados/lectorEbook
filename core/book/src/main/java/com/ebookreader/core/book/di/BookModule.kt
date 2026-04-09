package com.ebookreader.core.book.di

import com.ebookreader.core.book.scanner.BookScanner
import com.ebookreader.core.book.scanner.BookScannerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BookBindsModule {

    @Binds
    @Singleton
    abstract fun bindBookScanner(impl: BookScannerImpl): BookScanner
}
