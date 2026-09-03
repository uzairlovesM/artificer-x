package com.waheed.artificerx.di

import android.content.Context
import androidx.room.Room
import com.waheed.artificerx.data.repository.ChatWorkspaceRepository
import com.waheed.artificerx.data.workspace.ArtifactDao
import com.waheed.artificerx.data.workspace.MemoryDao
import com.waheed.artificerx.data.workspace.ChatMessageDao
import com.waheed.artificerx.data.workspace.ChatThreadDao
import com.waheed.artificerx.data.workspace.WorkspaceDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkspaceModule {
    @Provides
    @Singleton
    fun provideWorkspaceDatabase(@ApplicationContext context: Context): WorkspaceDatabase =
        Room.databaseBuilder(context, WorkspaceDatabase::class.java, "artificerx_workspace.db").build()

    @Provides fun provideChatThreadDao(db: WorkspaceDatabase): ChatThreadDao = db.chatThreadDao()
    @Provides fun provideChatMessageDao(db: WorkspaceDatabase): ChatMessageDao = db.chatMessageDao()
    @Provides fun provideArtifactDao(db: WorkspaceDatabase): ArtifactDao = db.artifactDao()
    @Provides fun provideMemoryDao(db: WorkspaceDatabase): MemoryDao = db.memoryDao()
}
