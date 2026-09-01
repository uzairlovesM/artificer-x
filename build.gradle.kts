plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.dependency.guard) apply false
    // versions (com.github.ben-manes.versions) is a project-wide
    // reporting plugin with no per-module config needed, so — unlike
    // every other plugin here, which is `apply false` at root and
    // actually applied down in app/build.gradle.kts — it's applied
    // directly here to register the root-level `dependencyUpdates`
    // task that dependencyUpdatesReport (below) depends on.
    alias(libs.plugins.versions)
    // dependency-analysis is also a project-wide plugin (it needs to
    // see every module's dependency graph at once to give "used
    // transitive dependency" advice), applied directly at root for the
    // same reason as `versions` above — registers `projectHealth`.
    alias(libs.plugins.dependency.analysis)
}

// Pin org.jetbrains.kotlin:* artifacts to this project's Kotlin version
// ACROSS THE APP'S OWN COMPILE/RUNTIME CLASSPATHS ONLY. This does not
// (and must not) touch the `detekt` classpath configuration: detekt
// bundles its own embedded Kotlin compiler and explicitly documents
// that forcing a newer Kotlin onto that classpath breaks it with
// "detekt was compiled with Kotlin X but is currently running with Y"
// (https://detekt.dev/docs/gettingstarted/gradle#dependencies) — which
// is exactly what happened when this force previously applied
// unconditionally via `allprojects { configurations.all { ... } } `.
// Excluding configurations named "detekt" keeps detekt on the Kotlin
// version it actually ships against while still pinning everything
// else the app itself compiles/runs with.
allprojects {
    configurations.all {
        // Any configuration detekt's Gradle plugin owns (its resolved
        // name varies by plugin version/module — e.g. "detekt",
        // "detektPlugins" — so match by substring rather than hardcode
        // one exact name) keeps its own declared Kotlin version.
        if (name.contains("detekt", ignoreCase = true)) return@all
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(libs.versions.kotlin.get())
                because(
                    "Force org.jetbrains.kotlin:* artifacts in the app's own build/runtime " +
                        "classpaths onto a single resolved Kotlin version, avoiding mixed-version " +
                        "classpath skew across modules. Excludes any detekt-owned configuration " +
                        "(see comment above) so detekt keeps using the Kotlin version it was built against.",
                )
            }
        }
    }
}

dependencyAnalysis {
    // Reduce noise for this project's actual shape: DataStore/Room/
    // Retrofit-style libraries are frequently flagged as "unused"
    // false positives because their usage is via generated code or
    // reflection the bytecode-level analyzer can't trace — rather
    // than accept every piece of advice blindly (which risks removing
    // a dependency KSP-generated code still needs), issues are
    // reported for review via `./gradlew :app:projectHealth`, never
    // auto-applied in CI. `all { }` applies this to every project
    // (just :app here, but keeps the config forward-compatible if a
    // second module is ever added).
    issues {
        all {
            onUnusedDependencies { severity("warn") }
            onUsedTransitiveDependencies { severity("warn") }
            onIncorrectConfiguration { severity("warn") }
        }
    }
}

// ── Root-level convenience tasks ──
// `./gradlew qualityGate` chains every static-analysis + test check the
// CI pipeline runs individually, so a Termux-local pre-push check can
// run the exact same gate in one command instead of remembering (and
// keeping in sync with) six separate gradlew invocations.
tasks.register("qualityGate") {
    group = "verification"
    description = "Runs ktlint, detekt, lint, and unit tests — the same gate CI enforces on every PR."
    dependsOn(
        ":app:ktlintCheck",
        ":app:detekt",
        ":app:lintDebug",
        ":app:testDebugUnitTest",
    )
}

// ── Dependency Locking ──
// CI's "Dependency Locking" step (.github/workflows/build.yml) runs
// `./gradlew dependencies --write-locks`, which is a silent no-op
// unless dependencyLocking is actually enabled somewhere in the build
// — without this block the step ran "successfully" every time while
// producing zero gradle.lockfile output, giving false confidence that
// dependency locking was active. lockAllConfigurations() locks every
// resolvable configuration across all subprojects; combined with the
// dependency-guard plugin (applied in app/build.gradle.kts) this gives
// two independent signals for an unexpected transitive-dependency
// change: a lockfile diff here, and a resolved-tree diff there.
allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}

// ── Dependency Analysis convenience task ──
// `./gradlew dependencyUpdatesReport` surfaces outdated libs.versions.toml
// entries in one report instead of manually checking each artifact's
// Maven Central page — genuinely useful on a project this dependency-heavy.
tasks.register("dependencyUpdatesReport") {
    group = "help"
    description = "Alias for dependencyUpdates (com.github.ben-manes.versions plugin) with a clearer name."
    dependsOn("dependencyUpdates")
}
