package com.waheed.artificerx.domain.usecase

import com.waheed.artificerx.core.creative.SceneCompositionEngine
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildCreativeUseCase @Inject constructor(private val composer: SceneCompositionEngine) {
    suspend fun build(request: String, vm: StudioViewModel): String = composer.compose(request, vm)
}
