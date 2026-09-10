package com.waheed.artificerx.init

import android.app.Application
import com.waheed.artificerx.core.ai.ReasoningEngine
import com.waheed.artificerx.util.PressureManager
import com.waheed.artificerx.util.NetworkManager
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object ArtificerXInit {
    private lateinit var context: Application
    private lateinit var interpreter: Interpreter
    private lateinit var networkManager: NetworkManager
    private lateinit var pressureManager: PressureManager

    fun init(application: Application) {
        context = application
        networkManager = NetworkManager(application)
        pressureManager = PressureManager()
        interpreter = loadTensorModel()
    }

    private fun loadTensorModel(): Interpreter {
        val modelFile = File(context.filesDir, "model_artificerx.tflite")
        val fileInputStream = context.contentResolver.openFileInput("model_artificerx.tflite").use {}
        return Interpreter(MappedByteBufferLoader(fileInputStream.fold)
        )
    }
}
