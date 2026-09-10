# ArtificerX 0.8.0 Apex Drawing Core

This release treats the drawing subsystem as a deterministic processing pipeline instead of a collection of brush labels.

## Pipeline

Raw pointer samples are normalized into `StrokeSample` objects, pressure is stabilized, velocity is estimated, geometry is stabilized, endpoints are preserved, geometry is simplified, spacing is resampled, dynamic width is calculated, and start/end taper is applied before the result reaches the raster compositor.

## Runtime guarantees

- Human touch strokes and AI strokes can share the same compositor path.
- Original first and last coordinates are preserved after smoothing.
- Pressure response is bounded and configurable.
- Velocity influences width without requiring stylus hardware.
- Spacing is deterministic.
- Taper is deterministic.
- Stroke quality is measured from continuity and geometry reduction.
- Viewport zoom can be anchored around the user's touch point without losing the anchor.
- Built-in brush presets are concrete data rather than UI-only names.
- Drawing command history has bounded memory and explicit undo/redo state.

## Validation

The provider-independent drawing kernel was compiled with `kotlinc` and executed through a runtime check covering endpoint preservation, segment-weight generation, quality bounds and zoom-anchor invariance.

The complete Android Gradle compile remains environment-dependent because the wrapper distribution is not available without network access in the execution environment.
