package com.waheed.artificerx.drawing

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.util.NativeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val nativeManager: NativeManager,
) : ViewModel() {
    private val _brushState = MutableStateFlow(BrushState(size = 24, color = 0xFF000000.toInt(), opacity = 1f))
    val brushState: StateFlow<BrushState> = _brushState

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _imageBitmap = MutableStateFlow<Bitmap?>(null)
    val imageBitmap: StateFlow<Bitmap?> = _imageBitmap

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    data class BrushState(val size: Int, val color: Int, val opacity: Float)

    fun processImage(imagePath: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _error.value = null
            _progress.value = 0f
            runCatching {
                nativeManager.getImageStats(imagePath)
                _progress.value = 0.35f
                nativeManager.processImage(imagePath)
            }.onSuccess { bitmap ->
                _imageBitmap.value = bitmap
                _progress.value = if (bitmap == null) 0f else 1f
            }.onFailure { error ->
                _error.value = error.message ?: "Image processing failed"
                _progress.value = 0f
            }
        }
    }

    fun updateBrush(size: Int, color: Int, opacity: Float) {
        _brushState.value = BrushState(size.coerceIn(1, 4096), color, opacity.coerceIn(0f, 1f))
    }

    fun clearCanvas() {
        _imageBitmap.value?.let { if (!it.isRecycled) it.recycle() }
        _imageBitmap.value = null
        _progress.value = 0f
        _error.value = null
    }

    override fun onCleared() {
        _imageBitmap.value?.let { if (!it.isRecycled) it.recycle() }
        super.onCleared()
    }
}
