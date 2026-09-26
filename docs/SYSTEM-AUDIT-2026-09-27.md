# ARTIFICER-X System Audit — 2026-09-27

## Audit coverage

This pass treated the project as a system rather than a single requested file. The working tree was read exhaustively at raw-byte and decoded-text level, then checked across Gradle/settings/plugin resolution, version authorities, Kotlin/Java local imports, Android/native build configuration, CI workflow logic, baseline-profile journeys, route/tool registries, source contracts, and project-specific foundation/APEX/deep validators.

Final pre-archive tree evidence:

- Files read: **3,086**
- Total bytes: **15,146,212**
- UTF-8 text files: **3,080**
- Decoded text lines: **315,681**
- Zero-byte files: **0**
- Invalid UTF-8 source/config files: **0**
- NUL-containing text files: **0**
- Case-collision paths: **0**
- Deterministic source-tree digest: `480975211aa6d9416fc45c3da7b25d804ba426fdc1ba3a9eb2a0a28d2641a070`

The final change set relative to the previous full-audit ZIP was **9 modified files + 5 new files**, with no removals.

## Build and system changes

- Added one Android toolchain authority in `gradle/libs.versions.toml` for compileSdk, minSdk, targetSdk, Build Tools, NDK, and CMake.
- Bound `app`, `:baselineprofile`, and `:benchmark` to those SDK and Build Tools authorities.
- Bound the app's CMake version through `android.externalNativeBuild.cmake.version`.
- Added the same explicit Build Tools version to all Android modules so CI and Gradle cannot drift.
- Removed duplicated SDK/NDK authority from `config/android-toolchain.json`; it now points back to the version catalog.
- Added `app/src/main/baseline-prof.txt` with a conservative startup/Studio seed.
- Made the app Baseline Profile consumer use `mergeIntoMain = true`, giving CI a stable `:app:generateBaselineProfile` task and one merged generated artifact.
- Upgraded the Baseline Profile generator to drive the real onboarding -> Studio/Canvas journey with UI Automator selectors and real canvas accessibility state.
- Updated the macrobenchmark to avoid measuring first-run onboarding as cold-start work.
- Added a CI Baseline Profile generation job with an emulator, KVM setup, exact SDK/native toolchain, generated-profile verification, and artifact upload.
- Scoped baseline CI to `androidx.benchmark.enabledRules=BaselineProfile` so unrelated benchmark rules are not run during profile generation.
- Added `tools/verify-build-contract.py` for cross-file SDK/native/plugin/baseline invariants.
- Added `tools/verify-catalog-usage.py` to validate every `libs.*` accessor and reject literal dependency/plugin versions in Gradle scripts.
- Added `tools/verify-local-imports.py` to validate project-owned Kotlin imports, including extension functions and generated Android `R`/`BuildConfig` exceptions.
- Added CI gates and final-summary/failure-counter entries for all three new contract checks.
- Updated the stale Kotlin compiler comment in `gradle.properties` so it describes the current toolchain instead of an obsolete compatibility ceiling.
- Updated the lint comment so the manual ART Baseline Profile is not confused with a lint suppression baseline.

## Version contract reviewed

The final project authority resolves to:

```text
Gradle               8.13
AGP                   8.13.2
Kotlin / KGP         2.3.10
KSP                   2.3.10
Compile SDK            36
Target SDK             36
Min SDK                33
Build Tools            36.0.0
NDK                    29.0.14206865
CMake                  3.31.6
Compose BOM            2026.09.00
AndroidX Benchmark     1.5.0
Baseline Profile       1.5.0
```

Current external evidence checked that Kotlin/KGP 2.3.10 is supported with AGP 8.13.x and Gradle 8.13, AGP 8.13.2 includes Kotlin 2.3 support, Benchmark 1.5.0 is the stable September 2026 release, and Compose BOM 2026.09.00 is published. No blind version bump was applied.

## Verification results

```text
CATALOG USAGE CONTRACT             PASS
LOCAL IMPORT CONTRACT              PASS
PLUGIN CLASSPATH CONTRACT          PASS
BUILD CONTRACT                     PASS
PROJECT VERIFICATION               PASS
FOUNDATION VERIFICATION            PASS
APEX RUNTIME VERIFICATION          PASS
DEEP PROJECT VERIFICATION          PASS
SOURCE CONTRACTS                   PASS
AI APEX VERIFICATION               PASS
PROJECT AUDIT                      PASS
INTEGRATION AUDIT                  PASS
EXTREME SOURCE AUDIT               PASS
ROUTE AUDIT                        PASS
RELEASE GATE                       PASS
PYTHON + SHELL SYNTAX              PASS
JSON/TOML/YAML/XML PARSE           PASS
FULL FILE READ                     PASS
```

Project metrics from the validators include 2,936 Kotlin sources, 86 declared routes, 0 unreferenced constants, 57 concrete tool schemas, 57 runtime tools, 5 Room entity tables, and 186 A-H runtime files.

The project is a marked private/personal build, so the existing ZIP still contains explicit signing material. The release gate reports that as a warning rather than a correctness failure because it is part of the existing private-build contract.

## External research used

- Android Baseline Profile manual rules: https://developer.android.com/topic/performance/baselineprofiles/manually-create-measure
- Android Baseline Profile generation: https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile
- Android Baseline Profile configuration: https://developer.android.com/topic/performance/baselineprofiles/configure-baselineprofiles
- Baseline Profile overview: https://developer.android.com/topic/performance/baselineprofiles/overview
- AGP 8.13 release notes: https://developer.android.com/build/releases/agp-8-13-0-release-notes
- Android Kotlin/AGP support: https://developer.android.com/build/kotlin-support
- Kotlin Gradle compatibility: https://kotlinlang.org/docs/gradle-configure-project.html
- Gradle plugin classpath behavior: https://docs.gradle.org/current/userguide/plugins.html
- Android Build Tools configuration: https://developer.android.com/tools/releases/build-tools
- Android CMake/NDK setup: https://developer.android.com/studio/projects/install-ndk
- Jetpack Benchmark releases: https://developer.android.com/jetpack/androidx/releases/benchmark
- UI Automator: https://developer.android.com/training/testing/other-components/ui-automator

Firecrawl and Parallel Search were used for current external research. Exa was also requested, but its current session returned HTTP 402 `credits limit exceeded`, so no Exa result is treated as evidence.

## Build limitation

A full Gradle Android compile/package was not executed in this sandbox because no compatible Gradle 8.13 distribution was cached locally and the environment cannot resolve/download `services.gradle.org`. The supplied GitHub CI logs provided the real failure reproduction for the earlier build break; the current ZIP is therefore backed by exhaustive source/config/static verification, not a fabricated local Gradle success claim.
