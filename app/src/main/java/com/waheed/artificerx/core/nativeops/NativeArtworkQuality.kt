package com.waheed.artificerx.core.nativeops

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

data class ArtworkQuality(val score: Int, val edgeEnergy: Float, val notes: List<String>)
@Singleton
class NativeArtworkQuality @Inject constructor() {
    fun evaluate(bitmap: Bitmap): ArtworkQuality {
        val raw = NativeRasterCore().analyze(bitmap)
        val edge = Regex("edge_energy=([0-9.]+)").find(raw)?.groupValues?.getOrNull(1)?.toFloatOrNull()?:0f
        val score = ((edge*180f)+35f).toInt().coerceIn(0, 100)
        val notes = buildList { if(score<30)add("Very flat or empty raster signal"); if(score in 30..55)add("Basic structure detected"); if(score>75)add("Strong edge/detail signal") }
        return ArtworkQuality(score, edge, notes)
    }
}
