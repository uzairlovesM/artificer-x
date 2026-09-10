package com.waheed.artificerx.init

import android.app.Application
import android.content.Context
import com.waheed.artificerx.util.PressureManager
import java.io.File

object ArtificerXInit {
    private var context: Application? = null
    private var modelFile: File? = null
    private var pressureManager: PressureManager? = null

    fun init(application: Application) {
        context = application
        pressureManager?.dispose()
        pressureManager = PressureManager()
        modelFile = File(application.filesDir, "model_artificerx.tflite").takeIf { it.isFile && it.canRead() }
    }

    fun isModelAvailable(): Boolean = modelFile?.canRead() == true
    fun modelPath(): String? = modelFile?.absolutePath
    fun applicationContext(): Context? = context
}
