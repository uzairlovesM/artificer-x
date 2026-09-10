package com.waheed.artificerx.core.agent

import android.graphics.Bitmap
import com.waheed.artificerx.core.ai.ReasoningEngine
import javax.inject.Inject
import javax.inject.Singleton

/** Provider-neutral remote bridge. Network/provider implementations can decorate
 * this class later without changing ReasoningEngine's dependency graph. */
@Singleton
class RemoteInferenceEngine @Inject constructor() {
    interface Callback

    fun generate(
        processed: Bitmap,
        prompt: String,
        config: ReasoningEngine.AgentConfig,
    ): Bitmap = processed.copy(processed.config ?: Bitmap.Config.ARGB_8888, true)

    fun releaseResources() = Unit
}
