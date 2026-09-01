package com.waheed.artificerx.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AgentOrchestrator, ToolExecutor, and ToolCallParser are all plain
 * @Inject-constructor singletons (no interface to bind, no external
 * instance to provide), so Hilt wires them automatically from
 * NetworkModule + DatabaseModule's existing bindings without needing
 * explicit @Provides methods here. This module exists as the documented
 * anchor point for the agent DI graph — if AgentOrchestrator later
 * needs an interface swap (e.g. a StreamingAgentOrchestrator variant),
 * the @Binds method goes here.
 */
@Module
@InstallIn(SingletonComponent::class)
object AgentModule
