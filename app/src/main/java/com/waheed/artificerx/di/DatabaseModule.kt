package com.waheed.artificerx.di

import android.content.Context
import androidx.room.Room
import com.waheed.artificerx.data.local.db.ArtificerXDatabase
import com.waheed.artificerx.data.local.db.ProjectDao
import com.waheed.artificerx.data.local.db.ProjectVersionDao
import com.waheed.artificerx.data.local.db.WorldModelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): ArtificerXDatabase =
        Room
            .databaseBuilder(context, ArtificerXDatabase::class.java, "artificerx.db")
            // No-arg overload: guaranteed stable across Room versions.
            // Drops + recreates every table on any unhandled schema
            // version bump. Fine for this personal-use build where the
            // local DB is a cache/project-store, not a source of truth
            // that needs versioned migrations yet.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProjectDao(database: ArtificerXDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideProjectVersionDao(database: ArtificerXDatabase): ProjectVersionDao = database.projectVersionDao()

    @Provides
    fun provideWorldModelDao(database: ArtificerXDatabase): WorldModelDao = database.worldModelDao()
}
