package com.waheed.artificerx.core.runtime

import com.waheed.artificerx.core.ai.runtime.CanvasOperationGuard
import com.waheed.artificerx.core.ai.runtime.CanvasOperationProposal
import com.waheed.artificerx.core.canvas.CanvasState
import com.waheed.artificerx.core.history.CanvasHistoryEngine
import com.waheed.artificerx.core.history.RecoveryJournal
import com.waheed.artificerx.core.project.ProjectGraph

/** Cross-subsystem coordinator: authoritative state, AI proposal gating, history and recovery. */
class CreativeRuntimeCoordinator(
    initialProject: com.waheed.artificerx.core.project.ProjectSnapshot,
    initialCanvas: CanvasState,
    private val guard: CanvasOperationGuard = CanvasOperationGuard(),
    private val history: CanvasHistoryEngine = CanvasHistoryEngine(),
    private val recovery: RecoveryJournal = RecoveryJournal(),
) {
    val projectGraph = ProjectGraph(initialProject)
    @Volatile private var canvasState = initialCanvas

    fun canvas(): CanvasState = canvasState
    fun history(): CanvasHistoryEngine = history
    fun recoveryJournal(): RecoveryJournal = recovery

    @Synchronized
    fun beginOperation(operationId: String, label: String): RecoveryJournal.Entry = recovery.append(operationId, RecoveryJournal.Phase.BEGIN, label)

    @Synchronized
    fun evaluateAiProposal(proposal: CanvasOperationProposal): com.waheed.artificerx.core.ai.runtime.OperationDecision =
        guard.evaluate(proposal, projectGraph.snapshot().metadata.revision, projectGraph.snapshot().activeLayer != null)

    @Synchronized
    fun markCanvasDirty(): CanvasState {
        canvasState = canvasState.markDirty()
        return canvasState
    }

    @Synchronized
    fun commitCanvasFingerprint(operationId: String, label: String, before: String, after: String, actor: String): CanvasHistoryEngine.Revision {
        require(before != after) { "No-op canvas commit" }
        val revision = CanvasHistoryEngine.Revision(label = label, beforeFingerprint = before, afterFingerprint = after, actor = actor)
        history.record(revision)
        recovery.append(operationId, RecoveryJournal.Phase.COMMIT, "$label|$before|$after")
        canvasState = canvasState.clean()
        return revision
    }

    @Synchronized
    fun rollback(operationId: String, reason: String) { recovery.append(operationId, RecoveryJournal.Phase.ROLLBACK, reason); canvasState = canvasState.clean() }
}
