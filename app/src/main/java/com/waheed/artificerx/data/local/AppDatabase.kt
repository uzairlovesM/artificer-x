package com.waheed.artificerx.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import com.waheed.artificerx.domain.Project
import com.waheed.artificerx.util.ProjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(entities = [Project::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Singleton
        @Provides
        fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                "artificerx_db"
            )
                .addMigrations(com.waheed.artificerx.data.local.DatabaseMigrations.MIGRATIONS)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
