package com.waheed.artificerx.ui.screens.art

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.ui.components.WorkspaceTopBar
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel

@Composable
fun ColorStudioScreen(vm: StudioViewModel, onBack:()->Unit){
 var hue by remember{mutableFloatStateOf(0f)}; var sat by remember{mutableFloatStateOf(1f)}; var value by remember{mutableFloatStateOf(1f)}; val palette = remember{mutableStateListOf("#E8DCC9", "#191918", "#D97757", "#8A6F52", "#D6B48A", "#6B7280", "#FFFFFF")}
 val c = Color.hsv(hue, sat, value)
 Scaffold(topBar = {WorkspaceTopBar("Color Studio", "HSV/HSL-style controls, palettes and quick color memory", onBack)}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)){
  Row(horizontalArrangement = Arrangement.spacedBy(10.dp)){Column(Modifier.size(120.dp).background(c)){ };Column{Text("${(hue).toInt()}°  ${(sat*100).toInt()}%  ${(value*100).toInt()}%");Button(onClick = {val hex = "#%02X%02X%02X".format((c.red*255).toInt(), (c.green*255).toInt(), (c.blue*255).toInt());palette.add(hex);vm.setBrushColor(hex)}){Text("Use + Save")}}}
  Text("Hue");Slider(hue, {hue = it}, valueRange = 0f..360f);Text("Saturation");Slider(sat, {sat = it}, valueRange = 0f..1f);Text("Value");Slider(value, {value = it}, valueRange = 0f..1f)
  Text("Palette");LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)){items(palette){hex->Button(onClick = {vm.setBrushColor(hex)}){Text(hex)}}}
 }}
}
