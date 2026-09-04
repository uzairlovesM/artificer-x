# ArtificerX 0.8.0 Upgrade

## Creative execution
- Added a semantic scene planner and deterministic `compose_scene` tool.
- Scene requests are decomposed into camera, palette, semantic layers, major objects, lighting and line-art passes.
- Added named-layer support in StudioViewModel so generated scenes are not flattened blobs.

## AI conversation system
- Added persistent chat profiles: Creative Studio, Engineering Agent, Research Analyst and Local Model.
- Profiles are stored locally and associated with individual conversation threads.
- Active profile controls temperature, provider/model preference and context strategy.
- Removed ArtificerX's fixed per-request `max_tokens` value. Provider/model output and context limits still apply externally and cannot be overridden by an Android client.

## AI environment awareness
- Added `inspect_android_toolchain` and AndroidToolchainManager.
- The agent can inspect Android SDK platforms, build tools, NDKs, CMake, Java, Git and ADB before build-oriented actions.
- Current project target: compile/target API 36, min API 33, NDK 29.0.14206865, arm64-v8a + x86_64.

## Drawing stack
- Added brush dynamics contracts for speed/pressure-aware size and opacity.
- Added advanced drawing feature contracts for stabilizer modes, symmetry, perspective guides, clipping, alpha lock, screen tone, selections, vector mode, grids and onion skin.
- Added local font assets: Noto Sans, Noto Sans Mono and DejaVu Sans Bold.

## Native stack
- Expanded the C++20 native module with a reusable raster edge-density kernel.
- Updated CMake to compile the additional native source.

## Architecture additions
- Added adaptive AI context use case, provider capability resolver, AI automation templates, unified repository navigator, capability use case, JSON sanitizer, local-model inspection and studio domain contracts.

## Verification
- project audit: PASS
- extreme source audit: PASS
- integration audit: PASS
- route audit: PASS
- release gate: PASS
- host C++ raster kernel compile: PASS
- complete Android Gradle compilation: NOT VERIFIED in sandbox because the Gradle distribution was not locally cached and network access to services.gradle.org was unavailable.
