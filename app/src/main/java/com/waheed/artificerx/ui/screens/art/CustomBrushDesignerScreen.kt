package com.waheed.artificerx.ui.screens.art

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.core.art.CustomBrushPreset
import com.waheed.artificerx.core.art.CustomBrushStore
import com.waheed.artificerx.domain.model.BrushType
import com.waheed.artificerx.ui.components.WorkspaceTopBar
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.util.UUID

@Composable
fun CustomBrushDesignerScreen(store: CustomBrushStore, onBack: () -> Unit) {
    var name by remember { mutableStateOf("My Brush") }
    var baseType by remember { mutableStateOf(BrushType.INK_PEN.name) }
    var size by remember { mutableFloatStateOf(24f) }
    var opacity by remember { mutableFloatStateOf(1f) }
    var hardness by remember { mutableFloatStateOf(.75f) }
    val presets = remember { mutableStateListOf<CustomBrushPreset>() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { presets.clear(); presets.addAll(store.list()) }
    val types = BrushType.entries.map { it.name }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        WorkspaceTopBar("Brush Designer", "Create, persist and reuse custom brush presets", onBack)
        OutlinedTextField(name, { name = it }, label={Text("Preset name")}, modifier=Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            types.take(8).forEach { type -> Button(onClick={baseType=type}) { Text(type.lowercase()) } }
        }
        Text("Size ${size.toInt()} px"); Slider(size, { size=it }, valueRange=1f..300f)
        Text("Opacity ${(opacity*100).toInt()}%"); Slider(opacity, { opacity=it }, valueRange=.05f..1f)
        Text("Hardness ${(hardness*100).toInt()}%"); Slider(hardness, { hardness=it }, valueRange=0f..1f)
        Button(onClick={
            scope.launch {
                val preset=CustomBrushPreset(UUID.randomUUID().toString(), name.ifBlank{"My Brush"}, baseType, size, opacity, hardness)
                store.save(preset); presets.clear(); presets.addAll(store.list())
            }
        }) { Text("Save preset") }
        Text("Saved presets", style=androidx.compose.material3.MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp), modifier=Modifier.fillMaxWidth().weight(1f)) {
            items(presets) { p -> Text("${p.name}  •  ${p.baseType}  •  ${p.size.toInt()}px  •  ${(p.opacity*100).toInt()}%") }
        }
    }
}
