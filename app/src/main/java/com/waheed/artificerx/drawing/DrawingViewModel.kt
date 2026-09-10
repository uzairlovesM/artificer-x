package com.waheed.artificerx.drawing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.runtime.NativeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val nativeManager: NativeManager
) : ViewModel() {
    private val _brushState = MutableStateFlow(BrushState())
    val brushState: StateFlow<BrushState> = _brushState

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _imageBitmap = MutableStateFlow<Bitmap?>(null)
    val imageBitmap: StateFlow<Bitmap?> = _imageBitmap

    data class BrushState(val size: Int, val color: Int, val opacity: Float)

    fun processImage(imagePath: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val stats = nativeManager.getImageStats(imagePath)
            _progress.value = 0.5f
            val bitmap = nativeManager.processImage(imagePath)
            _imageBitmap.value = bitmap
            _progress.value = 1.0f
        }
    }

    fun updateBrush(size: Int, color: Int, opacity: Float) {
        viewModelScope.launch {
            _brushState.value = _brushState.value.copy(
                size = size,
                color = color,
                opacity = opacity
            )
        }
    }

    fun clearCanvas() {
        viewModelScope.launch {
            _imageBitmap.value = null
            _progress.value = 0f
        }
    }
}