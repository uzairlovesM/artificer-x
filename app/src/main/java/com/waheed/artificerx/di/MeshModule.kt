package com.waheed.artificerx.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * SculptSceneStore, SculptBrushEngine, PrimitiveMeshGenerator, and
 * SculptToolExecutor are all @Inject-constructor singletons requiring
 * no interface binding — Hilt wires the entire 3D mesh-editing
 * subsystem automatically. No GPU renderer is wired yet (deliberately
 * deferred); when one is added it will attach as a pure reader of
 * SculptSceneStore and won't require changes to anything in this
 * module.
 */
@Module
@InstallIn(SingletonComponent::class)
object MeshModule
