package com.waheed.artificerx.core.diagnostics

enum class HealthStatus { OK, DEGRADED, FAILED }

data class HealthReport(
    val component: String,
    val status: HealthStatus,
    val message: String,
    val metrics: Map<String, Long> = emptyMap()
)

fun interface HealthCheck { fun run(): HealthReport }

class HealthRegistry {
    private val checks = LinkedHashMap<String, HealthCheck>()
    fun register(name: String, check: HealthCheck) { checks[name] = check }
    fun runAll(): List<HealthReport> = checks.map { (name, check) ->
        runCatching { check.run() }.getOrElse {
            HealthReport(name, HealthStatus.FAILED, it.message ?: it::class.java.simpleName)
        }
    }
}
