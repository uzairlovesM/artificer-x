package com.waheed.artificerx.core.ai.apex.permission

enum class AiPermission { READ_CANVAS, WRITE_CANVAS, READ_FILE, WRITE_FILE, NETWORK, DELETE_PROJECT, RUN_CODE, EXPORT }
data class AiPolicy(val granted: Set<AiPermission>, val confirmationRequired: Set<AiPermission> = emptySet()) {
    fun can(permission: AiPermission, confirmed: Boolean = false): Boolean = permission in granted && (permission !in confirmationRequired || confirmed)
}
class AiPermissionPolicy(initial: AiPolicy = AiPolicy(emptySet())) {
    private var policy = initial
    @Synchronized fun update(next: AiPolicy) { policy = next }
    @Synchronized fun can(permission: AiPermission, confirmed: Boolean = false): Boolean = policy.can(permission, confirmed)
    @Synchronized fun snapshot(): AiPolicy = policy
}
