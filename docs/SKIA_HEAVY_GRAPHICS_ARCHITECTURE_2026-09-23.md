# ARTIFICER-X Heavy Graphics Architecture, 2026-09-23

## Layered rendering strategy

1. **Compose / Android Canvas** remains the interactive on-screen layer.
2. **Skiko/Skia raster backend** handles large AI-generated paths and future offscreen export work.
3. **AGSL RuntimeShader** provides programmable bitmap effects on API 33+.
4. **Android 16 custom color/compositing effects** remain an opt-in extension point rather than a hard dependency.
5. **Capability policy** selects a backend by workload size and complexity and always falls back to the stable Canvas path.
6. **Macrobenchmark + Baseline Profile** provide measurable startup/frame-performance gates.

Skia is deliberately not injected into Compose's Android `ShaderBrush` pipeline. Skiko's Skia objects are treated as an offscreen/native rendering backend, preventing incompatible shader object mixing.

## Heavy-performance goals

- Large AI polyline batches: Skia offscreen raster path.
- Advanced filters: AGSL threshold, vignette, chromatic aberration, sharpen, posterize.
- Memory pressure: explicit bitmap cache budget.
- Performance evidence: Macrobenchmark 1.5.0 + Baseline Profile generator.
- Regression defense: existing source/runtime/release gates remain enabled.

## Fallback philosophy

Any Skia or AGSL initialization/render exception returns `false` and leaves the existing Android Canvas or ColorMatrix implementation in control. This prevents graphics-driver/library regressions from becoming an app-wide startup or drawing failure.

## Private signing material

The final private-use package intentionally preserves the source snapshot's signing keystore and `keystore_base64.txt` because the owner explicitly requested the private materials to remain in the personal archive. These materials must never be committed to a public GitHub branch or copied into GitHub Actions artifacts.

## Verified controls

- `tools/verify-project.py`: PASS
- `tools/verify-foundation.py`: PASS
- `tools/verify-apex-runtime.py`: PASS
- `tools/verify-deep-project.py`: PASS
- `tools/verify-source-contracts.py`: PASS
- `scripts/release_gate.py`: private archive mode is explicit through `PRIVATE_PERSONAL_BUILD` and does not weaken GitHub Actions secret detection.
