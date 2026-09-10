package com.waheed.artificerx.core.agent

enum class ToolRisk { LOW, MEDIUM, HIGH, CRITICAL }

data class ToolContract(
    val name: String,
    val description: String,
    val risk: ToolRisk,
    val requiresNetwork: Boolean = false,
    val requiresFileWrite: Boolean = false,
    val timeoutMillis: Long = 30_000L
) {
    init {
        require(name.matches(Regex("[a-zA-Z0-9_.:-]+")))
        require(description.isNotBlank())
        require(timeoutMillis in 100L..300_000L)
    }
}

class ToolPolicy {
    fun allowed(contract: ToolContract, network: Boolean, fileWrite: Boolean, maxRisk: ToolRisk): Boolean {
        if (contract.requiresNetwork && !network) return false
        if (contract.requiresFileWrite && !fileWrite) return false
        return contract.risk.ordinal <= maxRisk.ordinal
    }
}
