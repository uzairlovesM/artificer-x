from pathlib import Path
root=Path('/mnt/data/artificer_upgrade_work')
p=root/'app/src/main/java/com/waheed/artificerx/ui/screens/canvas/StudioViewModel.kt'
s=p.read_text()
needle='        fun setBrushSize(sizePx: Float) {'
insert='''        fun applyActiveFilter(filterType: String, intensity: Float = 1f) {
            val active = _state.value.activeLayerId ?: return
            val layer = _state.value.layers.firstOrNull { it.id == active } ?: return
            if (layer.isLocked) return
            bitmapStore.ensureLayer(active, _state.value.canvasWidthPx, _state.value.canvasHeightPx)
            bitmapStore.pushUndoSnapshot()
            if (compositor.applyFilter(active, filterType, intensity)) recomposite()
        }\n\n'''
if insert.strip() not in s:
    s=s.replace(needle,insert+needle)
p.write_text(s)

p=root/'app/src/main/java/com/waheed/artificerx/domain/model/CanvasModels.kt'
s=p.read_text()
s=s.replace('    val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,\n    val orderIndex: Int,', '    val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,\n    val orderIndex: Int,\n    val clipToBelow: Boolean = false,\n    val alphaLock: Boolean = false,\n    val isReference: Boolean = false,')
p.write_text(s)
