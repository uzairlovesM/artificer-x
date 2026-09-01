package com.waheed.artificerx.di

import com.waheed.artificerx.core.network.LLMAdapter
import com.waheed.artificerx.core.network.OpenAiCompatibleLLMAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    @Singleton
    abstract fun bindLLMAdapter(impl: OpenAiCompatibleLLMAdapter): LLMAdapter
}
