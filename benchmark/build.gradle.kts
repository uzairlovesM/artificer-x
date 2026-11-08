plugins {
    id("com.android.test")
    alias(libs.plugins.kotlin.android)
    // androidx.benchmark plugin removed: it only supports com.android.library modules,
    // not com.android.test. Macrobenchmarks run fine without it via
    // connectedBenchmarkAndroidTest — the plugin itself is not required.
}

android {
    namespace = "com.waheed.artificerx.benchmark"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    buildToolsVersion = libs.versions.androidBuildTools.get()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
}
