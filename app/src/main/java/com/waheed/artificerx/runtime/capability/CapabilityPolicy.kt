package com.waheed.artificerx.runtime.capability

enum class CapabilityRisk { READ_ONLY, LOCAL_WRITE, NETWORK, PROCESS, DEVICE_SENSITIVE }

data class CapabilityPolicy(val name: String, val risk: CapabilityRisk, val enabled: Boolean, val allowedScopes: Set<String>)
