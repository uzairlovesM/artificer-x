package com.waheed.artificerx.domain.model

data class StudioCapability(val id: String, val name: String, val enabled: Boolean, val source: String)
data class StudioUseCase(val id: String, val title: String, val description: String, val capabilities: List<String>)
