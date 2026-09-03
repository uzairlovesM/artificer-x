package com.waheed.artificerx.ui.screens.art

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
