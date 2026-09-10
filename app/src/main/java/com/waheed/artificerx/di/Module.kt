package com.waheed.artificerx.di

import com.waheed.artificerx.core.ai.ReasoningEngine
import com.waheed.artificerx.core.builtin.BuiltinRecipeCatalog
import com.waheed.artificerx.core.runtime.NetworkManager
import com.waheed.artificerx.core.runtime.PermissionManager
import com.waheed.artificerx.core.runtime.NativeManager
import com.waheed.artificerx.core.runtime.FileManager
import com.waheed.artificerx.core.runtime.CrashSafeSaveRegistry
import com.waheed.artificerx.core.runtime.AgentEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.*
import javax.inject.*

@Module
@InstallIn(SingletonComponent::class)
en class EngineModule {
    @Provides
    @Singleton
    fun provideAgentEventBus(): AgentEventBus = AgentEventBus

    @Provides
    @Singleton
    fun provideCrashSafeSaveRegistry(): CrashSafeSaveRegistry = CrashSafeSaveRegistry

    @Provides
    @Singleton
    fun provideNativeManager(): NativeManager = NativeManager() // Already Hilt-injectable

    @Provides
    @Singleton
    fun provideFileManager(): FileManager = FileManager() // Already Hilt-injectable

    @Provides
    @Singleton
    fun providePermissionManager(): PermissionManager = PermissionManager() // Inject via @ApplicationContext

    @Provides
    @Singleton
    fun provideNetworkManager(): NetworkManager = NetworkManager() // Inject via @ApplicationContext

    @Provides
    @Singleton
    fun provideBuiltinRecipeCatalog(): BuiltinRecipeCatalog = BuiltinRecipeCatalog

    @Provides
    @Singleton
    fun provideReasoningEngine(): ReasoningEngine = ReasoningEngine() // Hilt-injectable dependencies
}