package com.waheed.artificerx.ui.screens.art

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
fun BrushLabScreen(vm: StudioViewModel, onBack: () -> Unit, onCustomDesigner: () -> Unit = {}) {
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
            androidx.compose.material3.OutlinedButton(onClick = onCustomDesigner, modifier = Modifier.fillMaxWidth()) { Text("Open Custom Brush Designer") }
            Text("Size ${size.toInt()} px"); Slider(size, { size = it; vm.setBrushSize(it) }, valueRange = 1f..300f)
            Text("Opacity ${(opacity*100).toInt()}%"); Slider(opacity, { opacity = it; vm.setBrushDefaults(opacity = it) }, valueRange = 0.05f..1f)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(presets.take(120)) { preset -> Button(onClick = { vm.setBrushType(preset.type); vm.setBrushSize((24f*preset.sizeMultiplier).coerceIn(1f, 300f)); vm.setBrushDefaults(opacity = preset.opacity) }) { Text("${preset.name}  •  ${preset.family}  •  ${preset.type.name.lowercase()}") } } }
        }
    }
}
