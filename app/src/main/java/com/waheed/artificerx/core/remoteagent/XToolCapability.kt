package com.waheed.artificerx.core.remoteagent

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability
import java.util.concurrent.atomic.AtomicLong

/**
 * Runtime capability implementation for core.remoteagent.tool.
 *
 * This capability is stateful on purpose: it provides a deterministic lifecycle boundary
 * for callers that need to inspect readiness, execute work, observe failures, and recover
 * without coupling feature code to a UI or provider implementation.
 */
class XToolCapability : ExpansionCapability {
    override val id: String = "core.remoteagent.tool"
    override val area: String = "core.remoteagent"
    override val purpose: String = "Tool capability in core.remoteagent; explicit state, output, failure, telemetry, and provenance boundary."
    override val contracts: List<String> = listOf(
        "input",
        "state",
        "output",
        "failure",
        "telemetry",
        "provenance"
    )

    private val invocations = AtomicLong(0)
    private val successes = AtomicLong(0)
    private val failures = AtomicLong(0)
    private val configuration = LinkedHashMap<String, String>()
    private var active = false
    private var lastError: String? = null
    private var lastInputHash: Int = 0

    data class Snapshot(
        val id: String,
        val active: Boolean,
        val invocations: Long,
        val successes: Long,
        val failures: Long,
        val successRate: Float,
        val lastError: String?
    )

    data class Evaluation(
        val accepted: Boolean,
        val score: Float,
        val missing: List<String>,
        val warnings: List<String>
    )

    override fun validate(): CapabilityCheck {
        val missingContracts = contracts.filter { it.isBlank() }
        val duplicateCount = contracts.size - contracts.distinct().size
        val validIdentity = id.length >= 5 && area.isNotBlank() && purpose.length >= 20
        val ready = validIdentity && missingContracts.isEmpty() && duplicateCount == 0
        return CapabilityCheck(
            id = id,
            ready = ready,
            reason = when {
                !validIdentity -> "invalid-identity"
                missingContracts.isNotEmpty() -> "blank-contract"
                duplicateCount > 0 -> "duplicate-contract"
                else -> "runtime-ready"
            },
            signals = mapOf(
                "area" to area,
                "contractCount" to contracts.size.toString(),
                "distinctContracts" to contracts.distinct().size.toString(),
                "invocations" to invocations.get().toString(),
                "failures" to failures.get().toString(),
                "active" to active.toString()
            )
        )
    }

    @Synchronized
    fun activate(): Boolean {
        if (!validate().ready) return false
        active = true
        lastError = null
        return true
    }

    @Synchronized
    fun deactivate() {
        active = false
    }

    @Synchronized
    fun configure(values: Map<String, String>): Boolean {
        if (values.keys.any { it.isBlank() }) {
            lastError = "Configuration contains a blank key"
            return false
        }
        configuration.clear()
        values.forEach { (key, value) -> configuration[key] = value.trim().take(4096) }
        return true
    }

    @Synchronized
    fun evaluate(input: Map<String, String>): Evaluation {
        val missing = contracts
            .filter { it.endsWith(".input") || it.equals("input", ignoreCase = true) }
            .filter { contract -> input[contract]?.isNullOrBlank() != false }

        val warnings = mutableListOf<String>()
        if (input.isEmpty()) warnings += "empty-input"
        if (configuration.isEmpty()) warnings += "default-configuration"
        if (!active) warnings += "inactive"

        val completeness = if (contracts.isEmpty()) 0f
            else (contracts.count { contract -> input[contract]?.isNullOrBlank() == false }.toFloat() / contracts.size)

        val score = (completeness * .70f) +
            (if (active) .20f else 0f) +
            (if (configuration.isNotEmpty()) .10f else 0f)

        lastInputHash = input.entries
            .sortedBy { it.key }
            .fold(1) { hash, entry -> 31 * hash + (entry.key + "=" + entry.value).hashCode() }

        return Evaluation(
            accepted = active && missing.isEmpty() && score >= .50f,
            score = score.coerceIn(0f, 1f),
            missing = missing,
            warnings = warnings
        )
    }

    @Synchronized
    fun begin(input: Map<String, String>): Boolean {
        val evaluation = evaluate(input)
        if (!evaluation.accepted) {
            failures.incrementAndGet()
            lastError = "Input rejected: ${evaluation.missing.joinToString()}"
            return false
        }
        invocations.incrementAndGet()
        return true
    }

    @Synchronized
    fun complete() {
        successes.incrementAndGet()
        lastError = null
    }

    @Synchronized
    fun fail(message: String) {
        failures.incrementAndGet()
        lastError = message.take(2048)
    }

    @Synchronized
    fun resetRuntime() {
        invocations.set(0)
        successes.set(0)
        failures.set(0)
        lastError = null
        lastInputHash = 0
    }

    @Synchronized
    fun snapshot(): Snapshot {
        val calls = invocations.get()
        val ok = successes.get()
        return Snapshot(
            id = id,
            active = active,
            invocations = calls,
            successes = ok,
            failures = failures.get(),
            successRate = if (calls == 0L) 0f else (ok.toFloat() / calls).coerceIn(0f, 1f),
            lastError = lastError
        )
    }

    fun supports(contract: String): Boolean =
        contracts.any { it.equals(contract, ignoreCase = true) }

    fun configurationSnapshot(): Map<String, String> =
        synchronized(this) { configuration.toMap() }

    fun lastInputFingerprint(): Int = synchronized(this) { lastInputHash }

    fun normalizedId(): String = id.trim().lowercase()

    fun contractCoverage(input: Map<String, String>): Float {
        if (contracts.isEmpty()) return 1f
        return contracts.count { input[it]?.isNullOrBlank() == false }.toFloat() / contracts.size
    }
}
