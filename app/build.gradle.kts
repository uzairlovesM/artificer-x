import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
    alias(libs.plugins.dependency.guard)
    alias(libs.plugins.owasp.dependencycheck)
    alias(libs.plugins.license.report)
    alias(libs.plugins.dexcount)
    // Also applied at root (build.gradle.kts) — the dependency-analysis
    // plugin only auto-registers a `projectHealth` task on a subproject
    // if the plugin is applied to that subproject directly (applying it
    // at root alone only gets you the root-level `buildHealth`
    // aggregate task). CI calls `./gradlew :app:projectHealth`
    // (build.yml step "27e. Dependency Analysis"), which needs this
    // line to exist — without it that step fails with "Cannot locate
    // tasks that match ':app:projectHealth' as task 'projectHealth'
    // not found in project ':app'."
    alias(libs.plugins.dependency.analysis)
}

// ── local.properties loader ──
// project.findProperty() only reads Gradle properties (gradle.properties,
// -P flags, ORG_GRADLE_PROJECT_* env vars) — it does NOT read
// local.properties automatically outside of sdk.dir. API keys and signing
// credentials are intentionally kept out of gradle.properties (which is
// commonly committed) and instead live in the git-ignored local.properties,
// so we load that file explicitly here and fall back to CI secrets
// (injected as Gradle/env properties) when it's absent.
val localProps =
    Properties().apply {
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { load(it) }
        }
    }

fun resolveSecret(key: String): String? =
    localProps.getProperty(key)
        ?: (project.findProperty(key) as String?)
        ?: System.getenv(key)

// ── Kotlin compiler options (new DSL) ──
// kotlinOptions { jvmTarget = "17" } inside android{} is treated as a
// hard error under Kotlin 2.x's Gradle plugin, not merely a
// deprecation warning — this top-level `kotlin{}` block with
// compilerOptions is the required replacement. Declared here rather
// than nested in android{} because compilerOptions is a
// KotlinAndroidProjectExtension-level DSL, applying uniformly to every
// Kotlin compile task (debug, release, unit test, instrumented test)
// instead of needing to be repeated per variant.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}

android {
    namespace = "com.waheed.artificerx"
    // compileSdk 36 — required by androidx.core:core-ktx:1.18.0's AAR
    // metadata (and by androidx.core:core, its transitive dependency),
    // which enforces "compile against API 36+" regardless of what this
    // project itself targets. AGP 8.7.2 caps out at compileSdk 35, which
    // is why this was previously pinned to 35 with core-ktx *declared*
    // at 1.15.0 — but a transitive dependency was already resolving
    // core-ktx to 1.18.0 in practice, so the declared compileSdk and the
    // actually-enforced minimum had drifted apart. Bumped together with
    // the AGP version above (8.13.2, which supports up to API 36.1) so
    // declared and enforced values match again.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.waheed.artificerx"
        // Bumped from 26 -> 33: this is a personal, single-device app
        // (Waheed's own Android 13 phone per his own answer), so there's
        // no install-base reason to keep supporting API 26-32. API 33
        // unlocks the themed-icon, per-app-language, and granular-media
        // permission APIs the new screens below will want, without any
        // backward-compat shims.
        minSdk = 33
        // targetSdk intentionally stays one step behind compileSdk here:
        // compileSdk (compile against the newest APIs) and targetSdk
        // (opt in to that API level's *runtime behavior changes*) are
        // independent knobs — see AGP's own AAR-metadata note on this.
        // Targeting 35 while compiling against 36 avoids opting into
        // API-36 runtime behavior changes before they've been reviewed
        // against this app's own code, while still satisfying the
        // core-ktx 1.18.0 compileSdk requirement above.
        targetSdk = 35
        versionCode = 4
        versionName = "0.6.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "GROQ_API_KEY", "\"${resolveSecret("GROQ_API_KEY") ?: ""}\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"${resolveSecret("OPENROUTER_API_KEY") ?: ""}\"")
        buildConfigField("String", "CLOUDFLARE_API_TOKEN", "\"${resolveSecret("CLOUDFLARE_API_TOKEN") ?: ""}\"")
        buildConfigField("String", "CLOUDFLARE_ACCOUNT_ID", "\"${resolveSecret("CLOUDFLARE_ACCOUNT_ID") ?: ""}\"")

        // Room schema export (ArtificerXDatabase has exportSchema = true so
        // migrations can be tested against real historical schemas). Without
        // this arg, KSP has nowhere to write the exported schema JSON and
        // just warns + silently skips it every build.
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    // ── Release signing (personal build) ──
    // v1 (JAR signing) + v2 (APK Signature Scheme v2) + v3 (key rotation
    // support) are all enabled explicitly. v1 is kept alongside v2/v3
    // rather than relying on AGP defaults, since some install paths
    // (older package managers, certain OEM app-verification flows,
    // manual `pm install` on rooted/dev devices) still probe the v1
    // JAR signature block even on API 26+. v4 (fs-verity streaming) is
    // intentionally left off — it needs a companion .apk.idsig delivery
    // path that isn't relevant for a personal, non-Play-Store build.
    val releaseKeystoreFile = resolveSecret("RELEASE_STORE_FILE")
    signingConfigs {
        create("release") {
            if (releaseKeystoreFile != null) {
                storeFile = rootProject.file(releaseKeystoreFile)
                storePassword = resolveSecret("RELEASE_STORE_PASSWORD")
                keyAlias = resolveSecret("RELEASE_KEY_ALIAS")
                keyPassword = resolveSecret("RELEASE_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig =
                if (releaseKeystoreFile != null && rootProject.file(releaseKeystoreFile).exists()) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // ── Lint configuration ──
    // Without this block, any lint check AGP classifies as an "error"
    // severity (not just "warning") fails lintDebug/lintRelease outright
    // with no visibility into *which* checks fired or why — exactly the
    // kind of opaque failure the CI pipeline's step 9 previously hit.
    // abortOnError stays true (we want the pipeline to surface real
    // problems) but checkReleaseBuilds + the explicit report formats
    // below guarantee lint always emits a full HTML+SARIF+XML report
    // triplet regardless of pass/fail, and baseline.xml lets a
    // consciously-accepted pre-existing warning be silenced without
    // permanently disabling that check project-wide.
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        checkDependencies = true
        warningsAsErrors = false
        htmlReport = true
        // buildDir (the raw `File!` getter on Project) is deprecated —
        // Kotlin 2.1.20's Gradle-DSL script compiler now surfaces this
        // as a hard script-compilation error (not just a warning) the
        // moment any other genuine error exists in the same script,
        // which is what turned a cosmetic deprecation into a build
        // failure here. layout.buildDirectory is the official
        // replacement: a lazy DirectoryProperty (Provider-based) that
        // still resolves under the module's own build/ directory
        // exactly like the old $buildDir string interpolation did,
        // just without the deprecated eager File getter.
        htmlOutput =
            layout.buildDirectory
                .file("reports/lint/lint-report.html")
                .get()
                .asFile
        xmlReport = true
        xmlOutput =
            layout.buildDirectory
                .file("reports/lint/lint-report.xml")
                .get()
                .asFile
        sarifReport = true
        sarifOutput =
            layout.buildDirectory
                .file("reports/lint/lint-report.sarif")
                .get()
                .asFile
        textReport = true
        textOutput =
            layout.buildDirectory
                .file("reports/lint/lint-report.txt")
                .get()
                .asFile
        // No baseline file yet — this project has never had a full
        // lintRelease run in an environment with the Android SDK
        // available (this sandbox has none), so there's nothing real
        // to baseline. Once CI runs lintRelease for the first time,
        // generate one deliberately via `./gradlew lintRelease
        // -Dlint.baselines.continue=true` and point `baseline =` at
        // it — don't reference a file that was never actually
        // produced by a real lint run.
        // GradleDependency: this project intentionally pins dependency
        // versions by hand in libs.versions.toml (reviewed via the
        // `versions` plugin's dependencyUpdatesReport task) rather than
        // taking lint's "always use latest" suggestion automatically.
        // OldTargetApi: intentionally not on it — targetSdk tracks the
        // current stable Android release deliberately, one lint cycle
        // behind the newest preview/beta API level lint sometimes
        // flags as "old" the moment a new one enters beta.
        disable += setOf("GradleDependency", "OldTargetApi")
    }

    // ── Packaging ──
    // The OkHttp + Retrofit + Coil + Room + llama.cpp AAR stack
    // together pulls in several native .so libraries and duplicate
    // META-INF licence/notice files from transitive dependencies;
    // without excluding them, assembleRelease/assembleDebug fails at
    // the merge-native-libs / merge-java-resources step with a
    // "More than one file was found" duplicate-entry error the moment
    // two dependencies both ship e.g. META-INF/DEPENDENCIES.
    packaging {
        resources {
            excludes +=
                setOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "/META-INF/DEPENDENCIES",
                    "/META-INF/LICENSE",
                    "/META-INF/LICENSE.txt",
                    "/META-INF/LICENSE.md",
                    "/META-INF/LICENSE-notice.md",
                    "/META-INF/NOTICE",
                    "/META-INF/NOTICE.txt",
                    "/META-INF/NOTICE.md",
                    "/META-INF/*.kotlin_module",
                    "/META-INF/versions/9/previous-compilation-data.bin",
                    "META-INF/INDEX.LIST",
                    "META-INF/io.netty.versions.properties",
                    "win32-x86/**",
                    "win32-x86-64/**",
                )
            pickFirsts +=
                setOf(
                    "**/libc++_shared.so",
                    "**/libjsc.so",
                )
        }
        jniLibs {
            // llama.cpp's AAR ships native inference libraries per-ABI;
            // useLegacyPackaging keeps them uncompressed in the APK so
            // the OS can mmap the .so directly at load time instead of
            // extracting a multi-hundred-MB shared object to disk on
            // first run, which meaningfully speeds up local-model cold
            // start on lower-end devices.
            useLegacyPackaging = true
        }
    }

    // ── App Bundle / Split APK generation ──
    // Enables `./gradlew bundleRelease` to emit a proper .aab with
    // per-ABI/density/language splits, and is the input bundletool
    // needs to produce a .apks archive (Section: "apk aur .apks bnei").
    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    // ── Per-ABI split debug APKs ──
    // llama.cpp's native libraries roughly quadruple APK size across
    // arm64-v8a/armeabi-v7a/x86/x86_64 if shipped as one fat APK;
    // splitting here means CI's debug-APK artifact is the actual
    // per-device size a real install would be, not an inflated
    // universal APK that misrepresents it.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // ── AndroidX core ──
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.tracing)

    // ── Compose (bundle — see libs.versions.toml [bundles].compose-ui) ──
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose.ui)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ── Dependency injection ──
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler.androidx)

    // ── Persistence ──
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // ── Networking (bundle — see libs.versions.toml [bundles].networking) ──
    implementation(libs.bundles.networking)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.collections.immutable)
    // GraphQL client runtime, usable standalone (manual query
    // strings) without the Apollo Gradle codegen plugin — see
    // libs.versions.toml's plugins-section note on why the plugin
    // itself isn't applied yet.
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.normalized.cache)
    // WebSocket support: okhttp-core above already provides
    // okhttp3.WebSocket / WebSocketListener natively — no separate
    // dependency needed.

    // ── Web search & fetch — Section: Web search/fetch tools.
    // Powers WebSearchTool/HtmlFetcher agent tools: jsoup does the
    // HTML parsing, readability4j strips a fetched page down to its
    // readable article content (same job as Claude's own web_fetch
    // content-extraction, done on-device since this app has no
    // backend of its own to do it server-side). ──
    implementation(libs.bundles.web.fetch)

    // ── Camera / media capture — Section: Camera/media capture.
    // CameraX for photo/video capture (bundle — see
    // libs.versions.toml [bundles].camerax), Media3/ExoPlayer for
    // audio/video playback of captured or imported media. ──
    implementation(libs.bundles.camerax)
    implementation(libs.bundles.media3)

    // ── Maps / location — Section: Maps/location services.
    // osmdroid (no API key, offline-friendly OSM tiles) +
    // play-services-location for the device's own GPS/fused-location
    // fix — see libs.versions.toml's comment on why osmdroid over
    // the Google Maps SDK for this personal build. ──
    implementation(libs.osmdroid.android)
    implementation(libs.play.services.location)
    implementation(libs.androidx.preference)

    // ── File / document handling — Section: File/document handling.
    // PDF read/write (pdfbox-android), ZIP/archive handling
    // (commons-compress, also used by DocumentHandler's hand-rolled
    // DOCX writer — see libs.versions.toml's note on why POI/docx4j
    // are deliberately not used), and Storage-Access-Framework-aware
    // file references (documentfile) for the same
    // ACTION_OPEN_DOCUMENT-based pattern LocalModelRepository already
    // established for GGUF imports. ──
    implementation(libs.bundles.file.handling)

    // ── Painting/drawing — Section: Painting/drawing enhancement.
    // androidx.ink adds pressure/tilt-aware, low-latency stylus
    // strokes as a new brush engine option alongside the existing
    // hand-rolled Canvas brush system in SculptScreen/PaintStudio —
    // see libs.versions.toml [bundles].ink-drawing. ──
    implementation(libs.bundles.ink.drawing)

    // ── Images / rendering ──
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.androidsvg)
    implementation(libs.colorpicker.compose)
    implementation(libs.lottie.compose)
    implementation(libs.zoomable)
    implementation(libs.androidx.graphics.path)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.exifinterface)

    // ── Background work / security ──
    implementation(libs.work.runtime.ktx)
    implementation(libs.security.crypto)

    // ── Third-party UX ──
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)

    // ── Logging (debug-only) ──
    // ArtificerXApp.onCreate plants Timber.DebugTree only for debug
    // builds — release builds ship with no Timber tree registered
    // (log calls become no-ops), matching the release build's
    // lint-enforced no-raw-Log.* policy without needing a project-wide
    // migration off android.util.Log in the same change.
    implementation(libs.timber)
    debugImplementation(libs.leakcanary.android)

    // Local on-device GGUF + mmproj (vision) inference — Section: Local
    // Model provider. Prebuilt AAR from Maven Central; MIT-licensed
    // Kotlin bindings over a modernized llama.cpp core.
    implementation(libs.llamacpp.kotlin)

    // ── Unit tests (bundle — see libs.versions.toml
    // [bundles].unit-test-core) ──
    testImplementation(libs.bundles.unit.test.core)
    testImplementation(libs.room.testing)
    testImplementation(libs.work.testing)
    testImplementation(libs.okhttp.mockwebserver)

    // ── Instrumented tests (bundle — see libs.versions.toml
    // [bundles].instrumented-test-core) ──
    androidTestImplementation(libs.bundles.instrumented.test.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.mockk.android)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}

detekt {
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    // Section: system-files audit — parallel detekt across source sets
    // meaningfully speeds up `./gradlew detekt` on this project's ~90
    // Kotlin files without changing which rules run.
    parallel = true
}

ktlint {
    // Sourced from libs.versions.toml's ktlintCli entry rather than a
    // second hardcoded string literal, so bumping the ktlint CLI
    // version only ever needs one edit.
    version.set(libs.versions.ktlintCli.get())
    android.set(true)
    outputColorName.set("RED")
    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
    }
}

// ── Dependency Guard ──
// Section: system-files audit. Snapshots the fully-resolved dependency
// tree (including transitive deps) for the release runtime classpath
// into config/dependency-guard/*.txt on `./gradlew dependencyGuardBaseline`,
// then `./gradlew dependencyGuard` fails CI if that tree changes
// unexpectedly — catching a transitive-dependency version bump (e.g. a
// library silently pulling in a newer, possibly vulnerable, OkHttp)
// that a normal libs.versions.toml diff wouldn't surface on its own.
dependencyGuard {
    configuration("releaseRuntimeClasspath")
}

// ── Kover coverage report configuration ──
// Kover 0.8.0 removed the top-level `koverReport { }` extension
// function entirely (see kotlinx-kover's own 0.8.0 migration guide) —
// its content moved inside the `kover { }` extension's `reports { }`
// block. Applying the old `koverReport { }` syntax against Kover
// 0.9.1 (this project's pinned version) throws "Unresolved reference:
// koverReport" at script-compile time, since that top-level function
// no longer exists in the plugin's Gradle extension surface. The
// inner filters/excludes/classes/annotatedBy DSL is unchanged between
// 0.7.x and 0.9.x — only the outer wrapper needed updating.
kover {
    reports {
        filters {
            excludes {
                // Generated/boilerplate code that coverage numbers
                // shouldn't be penalized for: Hilt/Room codegen, DI
                // modules with no branching logic, Compose preview
                // functions, and BuildConfig.
                classes(
                    "*_HiltComponents*",
                    "*_Factory",
                    "*_MembersInjector",
                    "*Hilt_*",
                    "*.di.*",
                    "*ComposableSingletons*",
                    "*.BuildConfig",
                )
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
            }
        }
    }
}

// ── OWASP Dependency-Check ──
// Section: workflow/dependency additions. NVD-backed CVE scanner for
// the fully-resolved dependency tree. failBuildOnCVSS is set high
// (9.0, "critical" only) rather than failing on any finding — Android
// projects hit a well-documented false-positive pattern where the
// Kotlin standard library itself gets matched against unrelated CVEs
// by CPE heuristics, and a strict cutoff here would make every build
// red on day one. scanConfigurations restricts the scan to the
// actual shipped runtime classpath (skips test/androidTest/debug-only
// configurations), which is also the standard fix for the
// stdlib-false-positive problem — those configs pull in tooling
// (Kotlin compiler artifacts, JVM test runners) that only exist at
// build time and never ship in the APK, so a CVE against them is
// meaningless for what actually reaches a device.
dependencyCheck {
    failBuildOnCVSS = 9.0f
    formats = listOf("HTML", "JSON", "SARIF")
    scanConfigurations = listOf("releaseRuntimeClasspath")
    // pdfbox-android's own documented CVE caveat (see libs.versions.toml)
    // is the one known, already-reviewed finding this project ships
    // with anyway (personal, non-distributed build) — everything else
    // should genuinely fail attention, not get lost in that one
    // pre-accepted entry's noise.
    suppressionFile = "$rootDir/config/owasp/suppressions.xml"
    // NVD now requires a (free, self-service) API key for the CVE feed
    // updater — passing an *empty* apiKey is worse than not setting the
    // nvd{} block at all: the plugin sees a non-null key, tries to mask
    // it for logging, and fails immediately with "Invalid API Key,
    // length of 0 too short to provided a masked partial key" before
    // ever attempting a request. So only configure nvd{} when
    // NVD_API_KEY is actually present (wired from the NVD_API_KEY repo
    // secret in build.yml's OWASP step); otherwise leave the block
    // unset and let the plugin fall back to its own unauthenticated,
    // rate-limited default.
    System.getenv("NVD_API_KEY")?.takeIf { it.isNotBlank() }?.let { key ->
        nvd {
            apiKey = key
            delay = 4000
        }
    }
}

// ── License Report ──
// Section: workflow/dependency additions. Generates a full
// third-party license inventory under
// app/build/reports/dependency-license/ — genuinely useful given how
// many third-party libraries this project now pulls in (jsoup,
// readability4j, osmdroid, pdfbox-android, androidx.ink,
// llamacpp-kotlin, and more), each under its own license, for a
// project the user explicitly wants to keep personal/non-distributed
// but still wants an accurate record for.
licenseReport {
    // See lint{} block above for why $buildDir was replaced —
    // layout.buildDirectory is the non-deprecated equivalent.
    outputDir =
        layout.buildDirectory
            .dir("reports/dependency-license")
            .get()
            .asFile.path
}

// ── Dexcount ──
// Section: workflow/dependency additions ("app ka size bohot bada
// hona chahiye" — tracking growth, not preventing it). Reports method/
// field reference counts per build, letting size growth be observed
// as new feature domains (camera, maps, GraphQL, local-model
// inference) get wired in, without being a hard gate — minSdk 26 with
// automatic multidex means the historical 65,536-method ceiling this
// plugin was built to guard isn't a real constraint here.
// Configuration block intentionally omitted: dexcount 4.0.0's exact
// Kotlin-DSL property syntax (plain `=` vs `.set()`, and whether
// `format` takes an OutputFormat enum or a String) could not be
// confidently verified against a live build in this environment, and
// the plugin's own documented defaults (format = list, verbose =
// false) already match what this project wants — applying the plugin
// with no dexcount { } block at all avoids guessing at DSL syntax
// that could otherwise break the build on a wrong property type.
// Tune via an explicit block once verified against a real Gradle run.
