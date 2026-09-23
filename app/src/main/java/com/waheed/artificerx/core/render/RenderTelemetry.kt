package com.waheed.artificerx.core.render

import android.os.Trace
import javax.inject.Inject
import javax.inject.Singleton

/** Lightweight Perfetto/Systrace markers around expensive rendering stages. */
@Singleton
class RenderTelemetry @Inject constructor() {
    inline fun <T> section(name: String, block: () -> T): T {
        Trace.beginSection(name.take(120))
        return try {
            block()
        } finally {
            Trace.endSection()
        }
    }
}
