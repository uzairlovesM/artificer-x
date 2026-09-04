package com.waheed.artificerx.ui.screens.art

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
    val filters = listOf("grayscale", "invert", "saturation", "brightness", "contrast", "blur", "sharpen", "glitch", "chromatic aberration", "wave", "ripple", "swirl", "polar coordinates", "posterize", "halftone", "screen tone")
    var amount by remember { mutableFloatStateOf(1f) }
    Scaffold(topBar = {WorkspaceTopBar("Filter Lab", "Live non-destructive effect presets", onBack)}){pad->
        Column(Modifier.fillMaxSize().padding(pad).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)){
            Text("Intensity ${(amount*100).toInt()}%"); Slider(amount, {amount = it}, valueRange = .1f..2f)
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)){
                items(filters){ filter -> Button(onClick = {vm.applyActiveFilter(filter, amount)}, modifier = Modifier.fillMaxWidth()){Text(filter.replaceFirstChar{it.uppercase()})} }
            }
        }
    }
}
