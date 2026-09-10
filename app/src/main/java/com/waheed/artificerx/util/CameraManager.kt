package com.waheed.artificerx.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager,
) {
    private val _cameraAvailable = MutableStateFlow(false)
    val cameraAvailable: StateFlow<Boolean> = _cameraAvailable

    private val _previewSize = MutableStateFlow(Size(0, 0))
    val previewSize: StateFlow<Size> = _previewSize

    @SuppressLint("MissingPermission")
    fun startCamera(owner: LifecycleOwner, previewView: PreviewView) {
        if (!permissionManager.isPermissionGranted(android.Manifest.permission.CAMERA)) {
            _cameraAvailable.value = false
            return
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                val provider = future.get()
                provider.unbindAll()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { useCase ->
                        useCase.setAnalyzer(ContextCompat.getMainExecutor(context)) { image ->
                            _previewSize.value = Size(image.width, image.height)
                            image.close()
                        }
                    }
                provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                _cameraAvailable.value = true
            }.onFailure { _cameraAvailable.value = false }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() { _cameraAvailable.value = false }
}
