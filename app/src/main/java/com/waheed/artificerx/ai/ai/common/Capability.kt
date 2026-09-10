package com.waheed.artificerx.ai.ai.common

import android.content.Context
import android.graphics.Bitmap
import com.waheed.artificerx.ai.vision.VisionObservation

interface Capability<TInput, TOutput> {
    suspend fun process(input: TInput): TOutput
}

interface DrawingCapability : Capability<DrawingContext, DrawingResult>
interface VisionCapability : Capability<VisionInput, VisionAnalysis>
interface AudioCapability : Capability<TextInput, AudioClip>
interface TextCapability : Capability<Unit, String>

/** Provider-neutral input for the legacy AI drawing adapter. */
data class DrawingContext(
    val context: Context,
    val baseImage: Bitmap,
    val instruction: String = "",
    val metadata: Map<String, String> = emptyMap(),
)

data class DrawingResult(
    val image: Bitmap,
    val notes: List<String> = emptyList(),
    val confidence: Float = 1f,
)

data class VisionInput(val bitmap: Bitmap, val hint: String = "")
data class VisionAnalysis(val observation: VisionObservation, val summary: String)
data class TextInput(val text: String)
data class AudioClip(val bytes: ByteArray, val sampleRate: Int, val channels: Int)

interface ErrorHandler {
    fun handle(modelName: String, exception: Exception): List<ModelError>
}

data class ModelError(val model: String, val errorMessage: String?)

class MultipleModelFailureException(
    val errors: List<ModelError>,
) : Exception(errors.joinToString("; ") { "${it.model}: ${it.errorMessage ?: "unknown error"}" })
