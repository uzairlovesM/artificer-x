# CI Android SDK Fix

## Root cause

The failing GitHub Actions run explicitly requested the legacy Android SDK `tools` package through `android-actions/setup-android@v3`. Google no longer serves that package, so `sdkmanager tools` failed with `Failed to find package 'tools'` before Gradle started.

## Fix

- Upgrade `android-actions/setup-android` from `v3` to `v4`.
- Explicitly request only `platform-tools` from the setup action.
- Resolve `sdkmanager` through `PATH` instead of assuming a `cmdline-tools/latest` filesystem path.
- Remove the obsolete `tools` and `cmdline-tools;latest` install requests.
- Install the exact Android toolchain declared by the project:
  - Android platforms 35 and 36
  - Build Tools 35.0.0 and 36.0.0
  - NDK 29.0.14206865
  - CMake 3.31.6
- Add an immediate CI contract gate that verifies the exact SDK/NDK/CMake directories before Gradle starts.

## Verification

- YAML parse: PASS
- Deprecated `tools` package references in workflow: absent
- Project verification: PASS
- Foundation verification: PASS
- Apex runtime verification: PASS
- Deep project verification: PASS
- Source contract scan: PASS

The full Gradle/Android build was not executed in this sandbox because the sandbox does not provide the same GitHub-hosted Android runner environment.
