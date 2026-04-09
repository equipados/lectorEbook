package com.ebookreader.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.ebookreader.core.data.db.AppDatabase
import com.ebookreader.core.data.db.dao.BookDao
import com.ebookreader.core.data.db.dao.BookmarkDao
import com.ebookreader.core.data.preferences.UserPreferences
import com.ebookreader.core.data.preferences.UserPreferencesImpl
import com.ebookreader.core.data.repository.BookRepository
import com.ebookreader.core.data.repository.BookRepositoryImpl
import com.ebookreader.core.data.repository.BookmarkRepository
import com.ebookreader.core.data.repository.BookmarkRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

// ---------------------------------------------------------------------------
// Binds module — interfaces bound to their implementations
// ---------------------------------------------------------------------------

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {

    @Binds
    @Singleton
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferences(impl: UserPreferencesImpl): UserPreferences
}

// ---------------------------------------------------------------------------
// Provides module — concrete objects that need construction logic
// ---------------------------------------------------------------------------

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "ebook_reader.db"
    ).build()

    @Provides
    @Singleton
    fun provideBookDao(database: AppDatabase): BookDao =
        database.bookDao()

    @Provides
    @Singleton
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao =
        database.bookmarkDao()

    @Provides
    @Singleton
    fun provideUserDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.userDataStore
}
