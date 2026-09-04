package com.waheed.artificerx.core.creative

import com.waheed.artificerx.domain.model.BrushType
import com.waheed.artificerx.domain.model.DrawToolType
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class SceneCompositionEngine @Inject constructor(
    private val parser: SceneIntentParser,
) {
    suspend fun compose(request: String, vm: StudioViewModel): String {
        val blueprint = parser.parse(request)
        val width = max(1280, vm.state.value.canvasWidthPx)
        val height = max(960, vm.state.value.canvasHeightPx)
        vm.resizeCanvas(width, height)
        vm.setCanvasBackground(blueprint.palette.wall)

        val horizon = height * blueprint.camera.horizonRatio
        val vx = width * blueprint.camera.vanishingXRatio
        val floorY = height * 0.92f
        addLayer(vm, "01_Background")
        vm.setBrushDefaults(BrushType.MARKER, 18f, blueprint.palette.wall, 1f, 0.8f)
        vm.selectTool(DrawToolType.SHAPE_RECT)
        vm.drawManualShape(0f, 0f, width.toFloat(), horizon)
        vm.setBrushColor(blueprint.palette.floor)
        vm.drawManualShape(0f, horizon, width.toFloat(), floorY)

        addLayer(vm, "02_Architecture")
        vm.setBrushColor(blueprint.palette.trim)
        vm.setBrushType(BrushType.INK_PEN)
        vm.setBrushDefaults(sizePx=10f)
        vm.selectTool(DrawToolType.BRUSH)
        val perspectiveLines = listOf(
            listOf(0f,horizon, vx,horizon-5f), listOf(width.toFloat(),horizon,vx,horizon-5f),
            listOf(0f,floorY,width.toFloat(),floorY), listOf(0f,horizon,0f,floorY), listOf(width.toFloat(),horizon,width.toFloat(),floorY),
            listOf(width*0.5f,horizon,vx,floorY), listOf(width*0.12f,horizon,vx,floorY), listOf(width*0.88f,horizon,vx,floorY)
        )
        perspectiveLines.forEach { vm.drawManualStroke(it) }
        // window and room trims
        vm.selectTool(DrawToolType.SHAPE_RECT)
        vm.setBrushColor(blueprint.palette.ceiling)
        vm.drawManualShape(width*0.10f, height*0.15f, width*0.40f, height*0.42f)

        addLayer(vm, "03_Lighting")
        vm.setBrushDefaults(BrushType.AIRBRUSH, 120f, blueprint.palette.light, 0.35f, 0.5f)
        vm.selectTool(DrawToolType.BRUSH)
        vm.drawManualStroke(listOf(width*0.28f,height*0.18f,width*0.34f,height*0.52f,width*0.46f,height*0.70f))
        vm.setBrushDefaults(BrushType.AIRBRUSH, 160f, blueprint.palette.shadow, 0.22f, 0.4f)
        vm.drawManualStroke(listOf(width*0.08f,height*0.72f,width*0.32f,height*0.68f,width*0.60f,height*0.74f))

        addLayer(vm, "04_Furniture")
        blueprint.details.forEach { detail -> drawFurniture(vm, detail, width, height, blueprint.palette) }

        addLayer(vm, "05_Details")
        vm.setBrushDefaults(BrushType.INK_PEN, 6f, blueprint.palette.accent, 0.9f, 0.8f)
        vm.selectTool(DrawToolType.BRUSH)
        for (detail in blueprint.details) {
            val x = detail.x * width
            val y = detail.y * height
            vm.drawManualStroke(listOf(x-12f*detail.scale,y,x,y-16f*detail.scale,x+10f*detail.scale,y))
        }

        addLayer(vm, "06_LineArt")
        vm.setBrushDefaults(BrushType.INK_PEN, 7f, blueprint.palette.trim, 0.92f, 0.9f)
        vm.drawManualStroke(listOf(0f,0f,width.toFloat(),0f,width.toFloat(),height.toFloat(),0f,height.toFloat(),0f,0f))
        vm.drawManualStroke(listOf(0f,horizon,width.toFloat(),horizon))

        addLayer(vm, "07_Atmosphere")
        vm.setBrushDefaults(BrushType.AIRBRUSH, 220f, blueprint.palette.light, 0.08f, 0.2f)
        vm.drawManualStroke(listOf(width*0.25f,height*0.25f,width*0.40f,height*0.31f,width*0.56f,height*0.27f))

        return "Composed ${blueprint.style} scene '${blueprint.subject}' using ${blueprint.layers.size} semantic layers, a ${blueprint.camera.perspective.replace('_','-')} perspective scaffold, lighting and ${blueprint.details.size} scene elements."
    }

    private fun addLayer(vm: StudioViewModel, name: String) {
        vm.addNamedLayer(name)
        vm.state.value.activeLayerId?.let { vm.setLayerOpacity(it, 1f) }
    }

    private fun drawFurniture(vm: StudioViewModel, detail: SceneDetail, width: Int, height: Int, palette: PaletteSpec) {
        val x = detail.x * width
        val y = detail.y * height
        val s = detail.scale
        vm.selectTool(DrawToolType.SHAPE_RECT)
        when (detail.kind) {
            "window" -> { vm.setBrushColor("#D8ECFF"); vm.drawManualShape(x-110*s,y-110*s,x+110*s,y+30*s) }
            "desk" -> { vm.setBrushColor(palette.floor); vm.drawManualShape(x-150*s,y-50*s,x+150*s,y+50*s); vm.setBrushColor(palette.trim); vm.selectTool(DrawToolType.BRUSH); vm.drawManualStroke(listOf(x-120*s,y+50*s,x-100*s,y+180*s)); vm.drawManualStroke(listOf(x+120*s,y+50*s,x+100*s,y+180*s)) }
            "chair" -> { vm.setBrushColor(palette.accent); vm.selectTool(DrawToolType.SHAPE_RECT); vm.drawManualShape(x-70*s,y-40*s,x+70*s,y+90*s); vm.selectTool(DrawToolType.BRUSH); vm.drawManualStroke(listOf(x-55*s,y+90*s,x-45*s,y+170*s)); vm.drawManualStroke(listOf(x+55*s,y+90*s,x+45*s,y+170*s)) }
            "bed" -> { vm.setBrushColor("#D8B4C7"); vm.selectTool(DrawToolType.SHAPE_RECT); vm.drawManualShape(x-190*s,y-70*s,x+190*s,y+70*s); vm.setBrushColor("#F4E9EF"); vm.drawManualShape(x-170*s,y-55*s,x-50*s,y+45*s) }
            "plant" -> { vm.setBrushColor(palette.foliage); vm.selectTool(DrawToolType.BRUSH); vm.drawManualStroke(listOf(x,y+80*s,x-30*s,y-10*s,x-70*s,y-50*s)); vm.drawManualStroke(listOf(x,y+70*s,x+30*s,y-20*s,x+65*s,y-65*s)); vm.setBrushColor(palette.floor); vm.selectTool(DrawToolType.SHAPE_ELLIPSE); vm.drawManualShape(x-45*s,y+65*s,x+45*s,y+125*s) }
            "lamp" -> { vm.setBrushColor(palette.light); vm.selectTool(DrawToolType.SHAPE_ELLIPSE); vm.drawManualShape(x-55*s,y-40*s,x+55*s,y+40*s); vm.setBrushColor(palette.trim); vm.selectTool(DrawToolType.BRUSH); vm.drawManualStroke(listOf(x,y+40*s,x,y+150*s)) }
            "rug" -> { vm.setBrushColor("#C69ACF"); vm.selectTool(DrawToolType.SHAPE_ELLIPSE); vm.drawManualShape(x-170*s,y-55*s,x+170*s,y+55*s) }
        }
    }
}
