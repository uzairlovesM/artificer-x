package com.waheed.artificerx.ui.screens.art

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.ui.components.WorkspaceTopBar
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel

@Composable
fun LayerLabScreen(vm: StudioViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { WorkspaceTopBar("Layer Lab", "Locks, visibility, opacity, clipping and non-destructive controls", onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) { Text("${state.layers.size} layers", Modifier.weight(1f)); IconButton(onClick = {vm.addLayer()}){Icon(Icons.Filled.Add, "Add layer")} }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(state.layers.sortedByDescending { it.orderIndex }) { layer ->
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Row { Text(layer.name, Modifier.weight(1f)); Text(if (layer.id == state.activeLayerId) "ACTIVE" else "") }
                    Row { IconButton(onClick = {vm.setActiveLayer(layer.id)}) { Icon(Icons.Filled.Visibility, "Select layer") }; IconButton(onClick = {vm.toggleLayerVisibility(layer.id)}){Icon(Icons.Filled.Visibility, "Visibility")}; IconButton(onClick = {vm.toggleLayerLock(layer.id)}){Icon(Icons.Filled.Lock, "Lock")}; IconButton(onClick = { if (state.layers.size > 1) vm.deleteLayer(layer.id) }){Icon(Icons.Filled.Delete, "Delete")} }
                    Slider(layer.opacity, {vm.setLayerOpacity(layer.id, it)}, valueRange = 0f..1f)
                }
            } }
        }
    }
}
