# ARTIFICER-X Human Studio + AI Capability Upgrade

Date: 2026-09-24
Build: private personal-use build based on the Zero-Slack polish source

## What changed

### 1. Vision/model capability detection
- OpenRouter model discovery now consumes `architecture.input_modalities`, top-level modality fields, and supported parameters.
- A second OpenRouter catalog request using `input_modalities=image` cross-checks the provider catalog and marks matching models as vision-confirmed even when an individual `/models` record omits modality metadata.
- Vision, reasoning, and tool-capable model IDs discovered during setup are persisted in provider metadata, so capability behavior survives restart and model switching.
- Model cards now display capability evidence and input modalities rather than a single boolean badge.
- Existing name heuristics remain fallback-only and are explicitly labelled as inferred/unknown when metadata is not definitive.

### 2. Deeper AI reasoning UX
- Reasoning effort supports deeper presets and bounded provider summaries.
- The chat UI exposes a clickable Thinking panel with provider-supplied reasoning summaries, effort level, elapsed reasoning time, and executed tool information.
- Hidden private chain-of-thought is not exposed; only provider-supplied summaries and observable actions are shown.

### 3. Tool system
- Added `analyze_canvas`, which performs deterministic local visual inspection of the current canvas and returns scene type, composition/perspective scores, dominant palette, and actionable warnings.
- Added `suggest_palette`, producing HSL harmony palettes for complementary, analogous, triadic, split-complementary, tetradic, and monochrome workflows.
- `draw_path` now uses the advanced brush dynamics supplied by its tool arguments instead of silently falling back to flat strokes.
- AI-drawn strokes now use deterministic scatter, pressure, velocity, taper, smoothing, wetness, and bleed dynamics.
- Transform-based scaling is available as both an existing agent transform capability and a visible studio scaling UI.

### 4. Human-like drawing
- Added a deterministic HumanBrushEngine for pressure/velocity modulation and stroke tapering.
- Added deterministic spatial scatter so pencil/charcoal/dry media do not look like mathematically perfect vectors.
- Wetness and bleed influence stroke dynamics while preserving deterministic replay/undo behavior.
- Alpha-locked drawing supports the same point-weighted dynamics.
- Existing pressure simulation remains available for finger input without stylus hardware.

### 5. Canvas / Canva-style improvements
- Transform tool now exposes explicit 25%-400% layer scale presets centered on the canvas.
- Brush dock exposes size, opacity, hardness, flow, spacing, smoothing, scatter, pressure response, taper, wetness, and bleed.
- Color UI now includes editable hex input, curated swatches, and live HSL harmony palettes generated from the current color.
- Symmetry remains integrated with the same brush path pipeline.

## Verification

Passed:
- Project verification
- Deep project verification
- Source contract verification
- APEX runtime verification
- Foundation verification
- Pure Kotlin compilation of the new HumanBrushEngine and ColorPaletteEngine
- 2,935 Kotlin source files scanned

The Android Gradle compile was attempted but could not start because the sandbox could not download Gradle 8.13 from services.gradle.org. This is an environment/network limitation, not a claimed build pass.

## Private materials

The build intentionally retains the user's private-use signing materials:
- `keystore/artificerx-release.keystore.p12`
- `keystore_base64.txt`
- `PRIVATE_PERSONAL_BUILD`
- `research/source-packages/advanced_ui_design_system_research_2026.zip`

## Research basis

OpenRouter documentation states that `architecture.input_modalities` is the source of truth for image input, and also exposes `GET /api/v1/models?input_modalities=image` as a direct filter for vision-capable models. Current OpenRouter documentation also describes per-endpoint capability variation, so Artificer-X now treats provider metadata as authoritative where available and heuristics as fallback only.

Android / AndroidX references used for this upgrade include Material 3, Compose performance/stability, adaptive UI, accessibility, AndroidX Ink input dynamics, and current graphics guidance.

Exa was attempted in this project session but the connected Exa account returned HTTP 402 because its credit quota was exhausted, so no Exa result is represented as completed research.
