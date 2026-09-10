package com.waheed.artificerx.di

import android.content.Context
import com.waheed.artificerx.AgentEventBus
import com.waheed.artificerx.CrashSafeSaveRegistry
import com.waheed.artificerx.core.runtime.NetworkManager
import com.waheed.artificerx.util.FileManager
import com.waheed.artificerx.util.NativeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {
    @Provides @Singleton fun provideAgentEventBus(): AgentEventBus = AgentEventBus

    @Provides @Singleton fun provideCrashSafeSaveRegistry(): CrashSafeSaveRegistry =
        CrashSafeSaveRegistry

    @Provides @Singleton fun provideNetworkManager(
        @ApplicationContext context: Context,
    ): NetworkManager = NetworkManager(context)

    @Provides @Singleton fun provideFileManager(
        @ApplicationContext context: Context,
    ): FileManager = FileManager(context)

    @Provides @Singleton fun provideNativeManager(
        @ApplicationContext context: Context,
    ): NativeManager = NativeManager(context)

}
