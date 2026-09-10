package com.waheed.artificerx.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.CameraXConfig
import androidx.camera.core.impl.ImageAnalysisConfig
import androidx.camera.core.impl.Parameters
import androidx.camera.core.impl.UseCase
import androidx.camera.core.impl.utils.executor.CameraExecutor
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import com.waheed.artificerx.core.runtime.PermissionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedComponentScope
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext val context: Context,
    private val permissionManager: PermissionManager
) : LifecycleObserver, CameraExecutor() {

    // Camera state flows
    private val _cameraAvailable = MutableStateFlow(false)
    val cameraAvailable: StateFlow<Boolean> = _cameraAvailable

    private val _previewSize = MutableStateFlow(Size(0, 0))
    val previewSize: StateFlow<Size> = _previewSize

    @SuppressLint("RestrictedApi")
    fun startCamera(textureView: TextureView) {
        if (!permissionManager.isStoragePermissionGranted())
            return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider, textureView)
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("RestrictedApi")
    private fun bindPreview(cameraProvider: ProcessCameraProvider, textureView: TextureView) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(textureView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(context), ImageAnalyzer {
                    // Handle image analysis result
                })
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val camera = cameraProvider.bindToLifecycle(
            LifecycleOwnerProvider(textureView.context),
            cameraSelector,
            preview,
            imageAnalysis
        )
    }

    override fun shutdown() {
        super.shutdown()
        _cameraAvailable.value = false
    }
}