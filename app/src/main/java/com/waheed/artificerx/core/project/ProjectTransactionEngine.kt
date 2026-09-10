package com.waheed.artificerx.core.project

import com.waheed.artificerx.core.foundation.ExecutionId
import com.waheed.artificerx.core.operation.OperationContext
import com.waheed.artificerx.core.operation.OperationOutcome
import com.waheed.artificerx.core.operation.OperationRunner
import java.security.MessageDigest

/**
 * Canonical project mutation boundary for 1.0. It keeps mutations atomic from the caller's
 * perspective: validate -> mutate -> validate -> commit, with deterministic revision increments.
 */
class ProjectTransactionEngine(
    initial: ProjectSnapshot,
    private val validator: ProjectValidator = ProjectValidator(),
    private val runner: OperationRunner = OperationRunner(),
) {
    @Volatile private var snapshot = initial
    @Volatile private var dirty = !initial.saved

    init {
        require(validator.validate(initial).none { it.fatal }) { validator.validate(initial).joinToString { it.message } }
    }

    @Synchronized
    fun current(): ProjectSnapshot = snapshot

    @Synchronized
    fun isDirty(): Boolean = dirty

    @Synchronized
    fun <R> mutate(
        label: String,
        actor: String = "system",
        block: MutableProject.() -> R,
    ): OperationOutcome<MutationCommit<R>> {
        require(label.isNotBlank())
        val before = snapshot
        val mutable = MutableProject(before)
        val outcome = runner.run(
            context = OperationContext(actor = actor, metadata = mapOf("projectId" to before.metadata.id.value, "label" to label)),
            validate = { require(validator.validate(before).none { it.fatal }) }
        ) {
            val result = mutable.block()
            val candidate = mutable.build()
            val issues = validator.validate(candidate)
            require(issues.none { it.fatal }) { issues.filter { it.fatal }.joinToString { it.message } }
            candidate to result
        }
        if (!outcome.success) {
            return com.waheed.artificerx.core.operation.OperationOutcome(
                id = outcome.id,
                value = null,
                state = outcome.state,
                failure = outcome.failure,
                elapsedMillis = outcome.elapsedMillis,
                context = outcome.context,
            )
        }
        val pair = outcome.value ?: return com.waheed.artificerx.core.operation.OperationOutcome(
            id = outcome.id,
            value = null,
            state = com.waheed.artificerx.core.foundation.OperationState.FAILED,
            failure = com.waheed.artificerx.core.foundation.OperationFailure(
                com.waheed.artificerx.core.foundation.OperationFailureKind.INTERNAL,
                "Operation completed without a mutation payload",
                retryable = false,
            ),
            elapsedMillis = outcome.elapsedMillis,
            context = outcome.context,
        )
        snapshot = pair.first.copy(
            metadata = pair.first.metadata.copy(
                revision = before.metadata.revision + 1,
                modifiedAt = System.currentTimeMillis(),
            ),
            saved = false,
        )
        dirty = true
        return com.waheed.artificerx.core.operation.OperationOutcome(
            id = outcome.id,
            value = MutationCommit(label, before, snapshot, pair.second, sha256(snapshot)),
            state = outcome.state,
            failure = outcome.failure,
            elapsedMillis = outcome.elapsedMillis,
            context = outcome.context,
        )
    }

    @Synchronized
    fun markSaved(): ProjectSnapshot {
        snapshot = snapshot.copy(saved = true)
        dirty = false
        return snapshot
    }

    fun checksum(): String = sha256(current())

    fun executionId(): ExecutionId = ExecutionId.create()

    data class MutationCommit<R>(
        val label: String,
        val before: ProjectSnapshot,
        val after: ProjectSnapshot,
        val result: R,
        val checksum: String,
    )

    class MutableProject internal constructor(base: ProjectSnapshot) {
        private var metadata = base.metadata
        private val layers = base.layers.toMutableList()
        private var activeLayer = base.activeLayer

        fun rename(name: String) {
            require(name.isNotBlank())
            metadata = metadata.copy(title = name.trim())
        }

        fun setActiveLayer(id: LayerId?) {
            if (id != null) require(layers.any { it.id == id }) { "Layer does not exist: ${id.value}" }
            activeLayer = id
        }

        fun addLayer(layer: LayerDescriptor) {
            require(layers.none { it.id == layer.id }) { "Duplicate layer id: ${layer.id.value}" }
            layers.add(layer.copy(order = nextOrder()))
        }

        fun updateLayer(id: LayerId, transform: (LayerDescriptor) -> LayerDescriptor) {
            val index = layers.indexOfFirst { it.id == id }
            require(index >= 0) { "Layer does not exist: ${id.value}" }
            val next = transform(layers[index])
            require(next.id == id) { "Layer id cannot change during update" }
            layers[index] = next
        }

        fun removeLayer(id: LayerId) {
            val removed = layers.removeAll { it.id == id }
            require(removed) { "Layer does not exist: ${id.value}" }
            if (activeLayer == id) activeLayer = layers.maxByOrNull { it.order }?.id
        }

        fun build(): ProjectSnapshot = ProjectSnapshot(metadata, layers.sortedBy { it.order }, activeLayer, saved = false)
        private fun nextOrder(): Int = (layers.maxOfOrNull { it.order } ?: -1) + 1
    }

    private fun sha256(snapshot: ProjectSnapshot): String {
        val canonical = buildString {
            append(snapshot.metadata.id.value).append('|')
            append(snapshot.metadata.title).append('|')
            append(snapshot.metadata.width).append('x').append(snapshot.metadata.height).append('|')
            append(snapshot.metadata.createdAt).append('|').append(snapshot.metadata.modifiedAt).append('|')
            append(snapshot.metadata.colorProfile).append('|').append(snapshot.metadata.revision).append('|')
            append(snapshot.activeLayer?.value ?: "").append('|').append(snapshot.saved).append('\n')
            snapshot.layers.sortedBy { it.order }.forEach {
                append(it.id.value).append('|').append(it.name).append('|').append(it.visible).append('|')
                append(it.opacity).append('|').append(it.blendMode).append('|').append(it.locked).append('|').append(it.order).append('\n')
            }
        }
        return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
