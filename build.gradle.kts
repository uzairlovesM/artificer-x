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

// Pin every module's Kotlin Gradle plugin runtime jars (including the
// out-of-process compile daemon's own client/daemon jars) to a single
// resolved version via the official Kotlin BOM. Without this, Gradle
// 8.13 + Kotlin 2.3.10 can resolve the daemon-side and client-side
// Kotlin compiler jars to two different point releases (KGP publishes
// a matrix of Gradle-version-specific variants), and the mismatched
// pair throws `NoSuchMethodError` on
// `KotlinCompilerClient.connectAndLease` the moment the daemon
// connection handshake runs — i.e. every single compile. This is a
// known Kotlin 2.3.x + Gradle 8.13 interaction (JetBrains YouTrack
// KT-24735 and the wider "forward-compatibility violation" class of
// issue), not a project code bug.
allprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(libs.versions.kotlin.get())
                because(
                    "Force every org.jetbrains.kotlin:* artifact — including the compiler daemon's " +
                        "client/daemon jars — onto the single Kotlin version this project builds with, so the " +
                        "daemon handshake never mixes two point releases (see comment above).",
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
