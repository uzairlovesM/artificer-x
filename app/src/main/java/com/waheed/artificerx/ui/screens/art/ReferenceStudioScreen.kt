package com.waheed.artificerx.ui.screens.art

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.waheed.artificerx.ui.components.WorkspaceTopBar

@Composable
fun ReferenceStudioScreen(onBack:()->Unit){val refs = remember{mutableStateListOf<Uri>()};val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){it?.let(refs::add)};var active by remember{mutableStateOf<Uri?>(null)};Scaffold(topBar = {WorkspaceTopBar("Reference Studio", "Pinned visual references without flattening them into the artwork", onBack)}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)){Button(onClick = {picker.launch("image/*")}){Text("Add reference image")};active?.let{AsyncImage(model = it, contentDescription = "Reference", modifier = Modifier.fillMaxSize().weight(1f))};refs.forEach{u->Button(onClick = {active = u}){Text(u.lastPathSegment.orEmpty())}}}}}
