package com.waheed.artificerx.data.repository

import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.domain.model.StudioCapability
import com.waheed.artificerx.domain.repository.CapabilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalCapabilityRepository @Inject constructor(): CapabilityRepository {
    override fun observeCapabilities(): Flow<List<StudioCapability>> = flow {
        emit(ToolRegistry.ALL_TOOLS.map { StudioCapability(it.function.name, it.function.description, true, "builtin_or_runtime") })
    }
}
