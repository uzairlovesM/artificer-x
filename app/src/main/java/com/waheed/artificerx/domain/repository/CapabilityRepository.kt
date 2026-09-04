package com.waheed.artificerx.domain.repository

import com.waheed.artificerx.domain.model.StudioCapability
import kotlinx.coroutines.flow.Flow

interface CapabilityRepository { fun observeCapabilities(): Flow<List<StudioCapability>> }
