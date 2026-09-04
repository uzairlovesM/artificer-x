package com.waheed.artificerx.core.agent

/**
 * Decouples AgentOrchestrator from knowing whether it's driving the
 * 2D Studio or the 3D Sculpt scene — Section 156's vision-feedback
 * loop needs "a snapshot of whatever the agent is currently working
 * on, " and this interface is that seam. AgentChatViewModel supplies
 * the concrete implementation (StudioViewModel.captureSnapshotNow or
 * SculptViewModel.captureSnapshotNow) based on which screen the user
 * was on when they opened Agent Chat, so AgentOrchestrator's loop code
 * never branches on 2D-vs-3D itself.
 */
fun interface SnapshotProvider {
    fun captureSnapshot(): android.graphics.Bitmap?
}
