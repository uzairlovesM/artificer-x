package com.waheed.artificerx.ui.screens.art

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
 val rulers = listOf("Straight", "Circular", "Ellipse", "Radial", "Mirror", "Rotation", "Kaleidoscope", "Perspective", "Perspective Array", "Symmetry")
 var active by remember{mutableStateOf("Straight")}; var divisions by remember{mutableFloatStateOf(2f)}; var lock by remember{mutableStateOf(false)}
 Scaffold(topBar = {WorkspaceTopBar("Ruler & Perspective Lab", "Construction rulers, symmetry and perspective guides", onBack)}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)){
  rulers.chunked(2).forEach{row->Row(horizontalArrangement = Arrangement.spacedBy(7.dp)){row.forEach{r->FilterChip(selected = active==r, onClick = {active = r}, label = {Text(r)})}}}
  Text("Active: $active"); Text("Divisions ${divisions.toInt()}"); Slider(divisions, {divisions = it}, valueRange = 2f..24f, steps = 21)
  Row{Text("Lock ruler", Modifier.weight(1f));Switch(lock, {lock = it})}; Text(if(lock) "Guide is locked for drawing" else "Guide is editable")
 }}
}
