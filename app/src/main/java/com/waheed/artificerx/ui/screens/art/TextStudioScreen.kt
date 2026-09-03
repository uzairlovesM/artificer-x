package com.waheed.artificerx.ui.screens.art

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
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
fun TextStudioScreen(vm: StudioViewModel,onBack:()->Unit){var text by remember{mutableStateOf("ARTIFICER-X")};var size by remember{mutableFloatStateOf(48f)};var bold by remember{mutableStateOf(true)};Scaffold(topBar={WorkspaceTopBar("Typography Studio","Post-editable text layers and title effects",onBack)}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(text,{text=it},label={Text("Text")},modifier=Modifier.fillMaxWidth());Text("Font size ${size.toInt()} px");Slider(size,{size=it},valueRange=8f..220f);Row{Text("Bold",Modifier.weight(1f));Switch(bold,{bold=it})};Button(onClick={vm.addTextLayer(text,size,"#191918",bold)}){Text("Add live text layer")}}}}
