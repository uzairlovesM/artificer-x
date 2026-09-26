# ARTIFICER-X CI Build Fix 2026-09-25

## Root cause

GitHub Actions reached `assembleDebug`, but Gradle failed while configuring the
`:baselineprofile` module:

`Error resolving plugin [id: 'com.android.test', version: '8.13.2']`

`The request for this plugin could not be satisfied because the plugin is already
on the classpath with an unknown version, so compatibility cannot be checked.`

The project declared `com.android.test` only as a versioned version-catalog alias
inside the Android test modules. Another plugin path could place Android Gradle
Plugin classes on the buildscript classpath before that request was resolved,
leaving Gradle without a known plugin version for compatibility checking.

## Fix

1. Register `com.android.test` once in the root `build.gradle.kts` with `apply false`.
2. Apply `com.android.test` without a second version request in `:baselineprofile`
   and `:benchmark`.
3. Register every versioned external plugin used by a subproject at the root,
   including `androidx.benchmark`, `license-report`, and `dexcount`.
4. Add `tools/verify-plugin-classpath.py`, which fails early if a subproject adds
   a versioned plugin alias that is not registered at the root.
5. Run that invariant as CI step `1h`, before the expensive Gradle build steps.
6. Keep the Android SDK setup pinned to the project toolchain, including NDK
   `29.0.14206865` and CMake `3.31.6`, and never request the obsolete SDK `tools`
   package.

## Evidence

The supplied CI failure is the red reproduction. Local post-fix static and project
validators pass, including the new plugin-classpath invariant. A full Android
Gradle build is not claimed locally because the sandbox cannot resolve
`services.gradle.org` to download Gradle 8.13.
