# ArtificerX 0.9.0 Apex build-repair and subsystem upgrade

## KSP root-cause repairs

The project previously contained multiple incompatible persistence and runtime definitions. The canonical graph is now:

- `ArtificerXDatabase -> ProjectDao -> ProjectEntity`
- `ReasoningEngine -> NetworkManager + PermissionManager + LocalInferenceEngine + RemoteInferenceEngine`
- `DrawingViewModel -> util.NativeManager`
- Hilt DI uses constructor injection for concrete runtime services and explicit providers only where process-wide singleton objects require them.

The retired duplicate Room entity and DAO were removed from the active compilation graph. `domain.Project` is now a plain domain model and maps to `ProjectEntity`, so Room sees only one `projects` table.

## Runtime fixes

- Replaced malformed native image processing code with a real decode/copy/stats pipeline.
- Repaired permission management so `Application` is not incorrectly used as an `ActivityResultRegistryOwner`.
- Repaired CameraX integration to use `PreviewView` + `LifecycleOwner`.
- Repaired WorkManager unique periodic/immediate backup scheduling API usage.
- Replaced the malformed model bootstrap code with a safe optional model-file detector.
- Added a provider-neutral remote inference bridge so Hilt can resolve the reasoning graph without an absent class.
- Repaired image bitmap conversion utilities and removed an accidental duplicate Room view.

## Major/minor additions

- Added `CanvasInteractionCoordinator` for pan/zoom/rotate/mapping state.
- Added `BuildIntegrityGate` for deterministic SHA-256/runtime artifact verification.
- Added `tools/verify-project.py` and a CI pre-KSP source graph integrity gate.
- Bumped application version to `0.9.0-apex` / versionCode `9`.

## Validation

`tools/verify-project.py` passes with the repaired source tree. `git diff --check` passes. Pure Kotlin support classes compile independently with the installed Kotlin compiler.

A full Android Gradle/KSP execution could not be run in the current execution container because Gradle 8.13 is not cached and outbound DNS/network access to `services.gradle.org` is unavailable. GitHub Actions remains the authoritative full Android build environment.
