package com.waheed.artificerx.ai.common

interface Capability<TInput, TOutput> {
    suspend fun process(input: TInput): TOutput
}

interface DrawingCapability : Capability<DrawingContext, DrawingResult>
interface VisionCapability : Capability<VisionInput, VisionAnalysis>
interface AudioCapability : Capability<TextInput, AudioClip>
interface TextCapability : Capability<Unit, String>
