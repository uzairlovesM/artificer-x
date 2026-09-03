from pathlib import Path
root=Path('/mnt/data/artificer_upgrade_work')
def write(rel,text):
 p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)

write('app/src/main/java/com/waheed/artificerx/core/art/MaterialCatalog.kt', r'''package com.waheed.artificerx.core.art

data class MaterialPreset(val id: String, val name: String, val family: String, val scale: Float, val rotation: Float, val opacity: Float)

object MaterialCatalog {
    private val families = listOf("Paper", "Manga Tone", "Fabric", "Metal", "Stone", "Wood", "Leaves", "Cloud", "Glitter", "Noise", "Grid", "Dots", "Hatching", "Vintage", "Comic")
    val presets: List<MaterialPreset> = buildList {
        repeat(220) { index ->
            val family = families[index % families.size]
            add(MaterialPreset("material-${index.toString().padStart(4,'0')}", "$family ${index + 1}", family, 0.25f + (index % 16) / 10f, (index * 17 % 360).toFloat(), 0.35f + (index % 14) / 20f))
        }
    }
}
''')

write('app/src/main/java/com/waheed/artificerx/core/art/MangaLayoutModels.kt', r'''package com.waheed.artificerx.core.art

import kotlinx.serialization.Serializable

@Serializable
data class MangaPanel(val id: String, val x: Float, val y: Float, val width: Float, val height: Float, val border: Float = 6f, val caption: String = "")

@Serializable
data class MangaPageLayout(val pageId: String, val width: Int = 1600, val height: Int = 2200, val panels: List<MangaPanel> = emptyList(), val readingDirection: String = "LTR")
''')

write('app/src/main/java/com/waheed/artificerx/core/art/AnimationTimeline.kt', r'''package com.waheed.artificerx.core.art

import kotlinx.serialization.Serializable

@Serializable
data class AnimationFrameMeta(val index: Int, val durationMs: Int = 83, val hold: Boolean = false, val label: String = "Frame")

@Serializable
data class AnimationTimeline(val fps: Int = 12, val loop: Boolean = true, val onionSkin: Boolean = false, val frames: List<AnimationFrameMeta> = listOf(AnimationFrameMeta(0, label = "Frame 1")))
''')

write('app/src/main/java/com/waheed/artificerx/ui/screens/art/ProArtStudioScreen.kt', r'''package com.waheed.artificerx.ui.screens.art

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.domain.model.BrushType
import com.waheed.artificerx.domain.model.DrawToolType
import com.waheed.artificerx.ui.components.SoftCard
import com.waheed.artificerx.ui.components.WorkspaceChip
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProArtStudioScreen(vm: StudioViewModel, onBack: () -> Unit, onBrushes: () -> Unit, onLayers: () -> Unit, onFilters: () -> Unit, onRulers: () -> Unit, onAnimation: () -> Unit, onMaterials: () -> Unit, onManga: () -> Unit) {
    val state by vm.state.collectAsStateCompat()
    val bitmap by vm.compositedBitmap.collectAsStateCompat()
    var panel by remember { mutableStateOf("Canvas") }
    var zoom by remember { mutableFloatStateOf(1f) }
    Scaffold(topBar = { SmallTopAppBar(title = { Column { Text(state.projectName); Text("Pro art workspace", style = MaterialTheme.typography.labelSmall) } }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.Close, "Close") } }, actions = { IconButton(onClick = { vm.undo() }) { Icon(Icons.Filled.Remove, "Undo") }; IconButton(onClick = { vm.redo() }) { Icon(Icons.Filled.Add, "Redo") } }) }) { pad ->
        Row(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)) {
            Column(Modifier.width(80.dp).fillMaxHeight().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Canvas", "Brush", "Layers", "Color", "Tune").forEach { WorkspaceChip(it, selected = panel == it, onClick = { panel = it }) }
                Spacer(Modifier.weight(1f)); WorkspaceChip("AI", onClick = onBrushes)
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ToolAction("Brush", Icons.Filled.Brush) { vm.selectTool(DrawToolType.BRUSH) }
                    ToolAction("Erase", Icons.Filled.ContentCut) { vm.selectTool(DrawToolType.ERASER) }
                    ToolAction("Fill", Icons.Filled.Palette) { vm.selectTool(DrawToolType.FILL) }
                    ToolAction("Grid", Icons.Filled.GridOn) { vm.setCanvasBackground("#00000000") }
                    ToolAction("Filters", Icons.Filled.AutoFixHigh, onFilters)
                    ToolAction("Ruler", Icons.Filled.Tune, onRulers)
                    ToolAction("Layers", Icons.Filled.Layers, onLayers)
                    ToolAction("Anim", Icons.Filled.PlayArrow, onAnimation)
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
                        Canvas(Modifier.width(420.dp).height(520.dp).pointerInput(state.activeLayerId, zoom) {
                            detectDragGestures(
                                onDragStart = { start -> vm.drawManualStroke(listOf(start.x / zoom, start.y / zoom, start.x / zoom + .1f, start.y / zoom + .1f)) },
                                onDrag = { change, _ -> change.consume(); val x = change.position.x / zoom; val y = change.position.y / zoom; vm.drawManualStroke(listOf(x, y, x + .1f, y + .1f)) },
                            )
                        }) {
                            drawRect(color = androidx.compose.ui.graphics.Color(0xFFE9E5DF))
                            bitmap?.let { b -> drawImage(b.asImageBitmap(), dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())) }
                            if (state.toolState.pressureSimulationEnabled) {
                                drawCircle(androidx.compose.ui.graphics.Color.Black.copy(alpha = .08f), radius = 8.dp.toPx(), center = center)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { zoom = (zoom - .1f).coerceAtLeast(.5f) }) { Icon(Icons.Filled.Remove, "Zoom out") }
                    Slider(zoom, { zoom = it }, valueRange = .5f..2.5f, modifier = Modifier.weight(1f))
                    IconButton(onClick = { zoom = (zoom + .1f).coerceAtMost(2.5f) }) { Icon(Icons.Filled.Add, "Zoom in") }
                }
                AnimatedVisibility(panel != "Canvas") {
                    SoftCard(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text("$panel inspector", style = MaterialTheme.typography.titleMedium)
                        Text("Non-destructive controls stay attached to the live canvas session.", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WorkspaceChip("Brush Lab", onClick = onBrushes); WorkspaceChip("Materials", onClick = onMaterials); WorkspaceChip("Manga", onClick = onManga)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) { Row(Modifier.padding(horizontal=10.dp, vertical=7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(15.dp)); Text("  $label", style = MaterialTheme.typography.labelSmall) } }
}

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateCompat(): androidx.compose.runtime.State<T> = androidx.compose.runtime.collectAsStateWithLifecycle()
''')

write('app/src/main/java/com/waheed/artificerx/ui/screens/art/BrushLabScreen.kt', r'''package com.waheed.artificerx.ui.screens.art

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.core.art.BrushCatalog
import com.waheed.artificerx.domain.model.BrushType
import com.waheed.artificerx.ui.components.WorkspaceTopBar
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel

@Composable
fun BrushLabScreen(vm: StudioViewModel, onBack: () -> Unit) {
    val families = listOf("All") + BrushCatalog.presets.map { it.family }.distinct()
    var family by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    var size by remember { mutableFloatStateOf(vm.state.value.toolState.brushSizePx) }
    var opacity by remember { mutableFloatStateOf(vm.state.value.toolState.brushOpacity) }
    val presets = BrushCatalog.presets.filter { (family == "All" || it.family == family) && it.name.contains(query, true) }
    Scaffold(topBar = { WorkspaceTopBar("Brush Lab", "256 procedural presets + live brush parameters", onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { families.take(6).forEach { FilterChip(selected = family == it, onClick = { family = it }, label = { Text(it) }) } }
            OutlinedTextField(query, { query = it }, label = { Text("Search brushes") }, modifier = Modifier.fillMaxSize().weight(.0f))
            Text("Size ${size.toInt()} px"); Slider(size, { size=it; vm.setBrushSize(it) }, valueRange=1f..300f)
            Text("Opacity ${(opacity*100).toInt()}%"); Slider(opacity, { opacity=it; vm.setBrushDefaults(opacity=it) }, valueRange=0.05f..1f)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(presets.take(120)) { preset -> Button(onClick = { vm.setBrushType(preset.type); vm.setBrushSize((24f*preset.sizeMultiplier).coerceIn(1f,300f)); vm.setBrushDefaults(opacity=preset.opacity) }) { Text("${preset.name}  •  ${preset.family}  •  ${preset.type.name.lowercase()}") } } }
        }
    }
}
''')

write('app/src/main/java/com/waheed/artificerx/ui/screens/art/LayerLabScreen.kt', r'''package com.waheed.artificerx.ui.screens.art

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.ui.components.WorkspaceTopBar
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel

@Composable
fun LayerLabScreen(vm: StudioViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateCompat2()
    Scaffold(topBar = { WorkspaceTopBar("Layer Lab", "Locks, visibility, opacity, clipping and non-destructive controls", onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) { Text("${state.layers.size} layers", Modifier.weight(1f)); IconButton(onClick={vm.addLayer()}){Icon(Icons.Filled.Add,"Add layer")} }
            LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) { items(state.layers.sortedByDescending { it.orderIndex }) { layer ->
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Row { Text(layer.name, Modifier.weight(1f)); Text(if (layer.id == state.activeLayerId) "ACTIVE" else "") }
                    Row { IconButton(onClick={vm.setActiveLayer(layer.id)}) { Icon(Icons.Filled.Visibility,"Select layer") }; IconButton(onClick={vm.toggleLayerVisibility(layer.id)}){Icon(Icons.Filled.Visibility,"Visibility")}; IconButton(onClick={vm.toggleLayerLock(layer.id)}){Icon(Icons.Filled.Lock,"Lock")}; IconButton(onClick={if(state.layers.size>1){{vm.deleteLayer(layer.id)}} else { { } }}){Icon(Icons.Filled.Delete,"Delete")} }
                    Slider(layer.opacity,{vm.setLayerOpacity(layer.id,it)}, valueRange=0f..1f)
                }
            } }
        }
    }
}

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateCompat2() = androidx.lifecycle.compose.collectAsStateWithLifecycle()
''')

write('app/src/main/java/com/waheed/artificerx/ui/screens/art/FilterLabScreen.kt', r'''package com.waheed.artificerx.ui.screens.art

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.ui.components.WorkspaceTopBar
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel

@Composable
fun FilterLabScreen(vm: StudioViewModel, onBack: () -> Unit) {
    val filters = listOf("grayscale","invert","saturation","brightness","contrast","blur","sharpen","glitch","chromatic aberration","wave","ripple","swirl","polar coordinates","posterize","halftone","screen tone")
    var amount by remember { mutableFloatStateOf(1f) }
    Scaffold(topBar={WorkspaceTopBar("Filter Lab","Live non-destructive effect presets",onBack)}){pad->
        Column(Modifier.fillMaxSize().padding(pad).padding(14.dp), verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("Intensity ${(amount*100).toInt()}%"); Slider(amount,{amount=it}, valueRange=.1f..2f)
            LazyVerticalGrid(columns=GridCells.Fixed(2), modifier=Modifier.weight(1f), horizontalArrangement=Arrangement.spacedBy(8.dp), verticalArrangement=Arrangement.spacedBy(8.dp)){
                items(filters){ filter -> Button(onClick={vm.applyActiveFilter(filter,amount)}, modifier=Modifier.fillMaxWidth()){Text(filter.replaceFirstChar{it.uppercase()})} }
            }
        }
    }
}
''')

write('app/src/main/java/com/waheed/artificerx/ui/screens/art/RulerLabScreen.kt', r'''package com.waheed.artificerx.ui.screens.art

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.ui.components.WorkspaceTopBar

@Composable
fun RulerLabScreen(onBack:()->Unit){
 val rulers=listOf("Straight","Circular","Ellipse","Radial","Mirror","Rotation","Kaleidoscope","Perspective","Perspective Array","Symmetry")
 var active by remember{mutableStateOf("Straight")}; var divisions by remember{mutableFloatStateOf(2f)}; var lock by remember{mutableStateOf(false)}
 Scaffold(topBar={WorkspaceTopBar("Ruler & Perspective Lab","Construction rulers, symmetry and perspective guides",onBack)}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  rulers.chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){row.forEach{r->FilterChip(selected=active==r,onClick={active=r},label={Text(r)})}}}
  Text("Active: $active"); Text("Divisions ${divisions.toInt()}"); Slider(divisions,{divisions=it}, valueRange=2f..24f, steps=21)
  Row{Text("Lock ruler",Modifier.weight(1f));Switch(lock,{lock=it})}; Text(if(lock) "Guide is locked for drawing" else "Guide is editable")
 }}
}
''')

write('app/src/main/java/com/waheed/artificerx/ui/screens/art/AnimationLabScreen.kt', r'''package com.waheed.artificerx.ui.screens.art

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.ui.components.WorkspaceTopBar

@Composable
fun AnimationLabScreen(onBack:()->Unit){
 var fps by remember{mutableIntStateOf(12)}; var frames by remember{mutableIntStateOf(1)}; var current by remember{mutableIntStateOf(0)}; var onion by remember{mutableStateOf(false)}
 Scaffold(topBar={WorkspaceTopBar("Animation Timeline","Flipbook frames, FPS, onion skin and holds",onBack)}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text("FPS $fps"); Slider(fps.toFloat(),{fps=it.toInt()},valueRange=1f..60f,steps=59)
 Row{Text("Onion skin",Modifier.weight(1f));Switch(onion,{onion=it})}; Text("Frame ${current+1} / $frames")
 LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items((0 until frames).toList()){i->Button(onClick={current=i}){Text("${i+1}")}}}
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={frames++;current=frames-1}){Text("+ Frame")}; Button(onClick={if(frames>1){frames--;current=current.coerceAtMost(frames-1)}}){Text("Remove")}}
 Text("Current frame duration ${1000/fps} ms")
 }}
}
''')

write('app/src/main/java/com/waheed/artificerx/ui/screens/art/MaterialLabScreen.kt', r'''package com.waheed.artificerx.ui.screens.art

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.core.art.MaterialCatalog
import com.waheed.artificerx.ui.components.WorkspaceTopBar

@Composable
fun MaterialLabScreen(onBack:()->Unit){val families=MaterialCatalog.presets.map{it.family}.distinct();var family by remember{mutableStateOf(families.first())};val items=MaterialCatalog.presets.filter{it.family==family};Scaffold(topBar={WorkspaceTopBar("Material Studio","Procedural textures, manga tones and surface recipes",onBack)}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(14.dp)){androidx.compose.foundation.lazy.LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(families){f->FilterChip(selected=family==f,onClick={family=f},label={Text(f)})}};LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){items(items){m->Text("${m.name} • scale ${"%.2f".format(m.scale)} • opacity ${"%.2f".format(m.opacity)}",Modifier.padding(8.dp))}}}}}
''')

write('app/src/main/java/com/waheed/artificerx/ui/screens/art/MangaPageLabScreen.kt', r'''package com.waheed.artificerx.ui.screens.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.ui.components.WorkspaceTopBar

@Composable
fun MangaPageLabScreen(onBack:()->Unit){
 val panels=remember{mutableStateListOf(Rect(.1f,.1f,.9f,.35f),Rect(.1f,.4f,.48f,.9f),Rect(.52f,.4f,.9f,.9f))};var mode by remember{mutableStateOf(false)}
 Scaffold(topBar={WorkspaceTopBar("Manga Page Studio","Panels, gutters, captions and reading flow",onBack)}){pad->Column(Modifier.fillMaxSize().padding(pad),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Row(Modifier.padding(10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={panels.add(Rect(.15f,.15f,.5f,.4f))}){Text("Add panel")};Button(onClick={if(panels.size>1)panels.removeLast()}){Text("Remove")};Button(onClick={mode=!mode}){Text(if(mode)"RTL" else "LTR")}}
  Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center){Canvas(Modifier.width(340.dp).height(500.dp).background(Color.White).pointerInput(panels.size){detectDragGestures{change,_->change.consume()}}){panels.forEach{p->drawRect(Color.Black,topLeft=androidx.compose.ui.geometry.Offset(p.left*size.width,p.top*size.height),size=androidx.compose.ui.geometry.Size(p.width*size.width,p.height*size.height),style=androidx.compose.ui.graphics.drawscope.Stroke(width=4f))}}}
 }}
}
''')
