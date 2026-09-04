package com.waheed.artificerx.ui.screens.art

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.ui.components.WorkspaceTopBar

@Composable
fun MangaPageLabScreen(onBack:()->Unit){
 val panels = remember{mutableStateListOf(Rect(.1f, .1f, .9f, .35f), Rect(.1f, .4f, .48f, .9f), Rect(.52f, .4f, .9f, .9f))};var mode by remember{mutableStateOf(false)}
 Scaffold(topBar = {WorkspaceTopBar("Manga Page Studio", "Panels, gutters, captions and reading flow", onBack)}){pad->Column(Modifier.fillMaxSize().padding(pad), verticalArrangement = Arrangement.spacedBy(8.dp)){
  Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)){Button(onClick = {panels.add(Rect(.15f, .15f, .5f, .4f))}){Text("Add panel")};Button(onClick = {if(panels.size>1)panels.removeLast()}){Text("Remove")};Button(onClick = {mode = !mode}){Text(if(mode)"RTL" else "LTR")}}
  Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center){Canvas(Modifier.width(340.dp).height(500.dp).background(Color.White).pointerInput(panels.size){detectDragGestures{change, _->change.consume()}}){panels.forEach{p->drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(p.left*size.width, p.top*size.height), size = androidx.compose.ui.geometry.Size(p.width*size.width, p.height*size.height), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))}}}
 }}
}
