# AI Creative Pipeline

The agent now separates semantic intent from pixel execution. Scene requests can be compiled into a structured SceneBlueprint containing camera, palette, semantic layers and scene details. `compose_scene` executes a deterministic multi-layer raster build, after which the agent can inspect the canvas and repair it.

This avoids treating “anime room” as an unconstrained freehand sketch. The prompt is decomposed into spatial primitives, major furniture, lighting, perspective lines and line-art passes.

The local model path remains user-supplied and offline-capable. Cloud providers remain provider-specific and may still enforce their own context/output ceilings. ArtificerX does not add a per-message `max_tokens` parameter.
