package com.waheed.artificerx.ui.screens.art

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
