package com.waheed.artificerx.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.ui.components.SoftCard
import com.waheed.artificerx.ui.components.WorkspaceTopBar

@Composable
fun AgentWorkbenchScreen(onBack:()->Unit,onChat:()->Unit,onTools:()->Unit){
 var planning by remember{mutableStateOf(true)};var memory by remember{mutableStateOf(true)};var artifacts by remember{mutableStateOf(true)};var selfCorrect by remember{mutableStateOf(true)};var web by remember{mutableStateOf(false)};var budget by remember{mutableFloatStateOf(0.72f)};var maxTools by remember{mutableIntStateOf(32)}
 val confidence by animateFloatAsState(if(planning && memory && selfCorrect) .96f else .65f,label="confidence")
 Scaffold(topBar={WorkspaceTopBar("Agent Workbench","Claude-inspired reasoning workspace and execution controls",onBack)}){pad->
  Column(Modifier.fillMaxSize().padding(pad).padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   SoftCard{Text("Reasoning profile",style=androidx.compose.material3.MaterialTheme.typography.titleLarge);Text(if(planning)"Plan → execute → inspect → self-correct → finalize" else "Direct execution mode");LinearProgressIndicator({confidence},Modifier.fillMaxWidth())}
   SoftCard{Setting("Planning loop","Build a plan before changing files",planning){planning=!planning};Setting("Memory","Recall persistent project/user memory",memory){memory=!memory};Setting("Artifact-first","Prefer files/ZIP/images over prose-only replies",artifacts){artifacts=!artifacts};Setting("Self-correction","Retry failed tools with revised arguments",selfCorrect){selfCorrect=!selfCorrect};Setting("Web tools","Allow web search/fetch capabilities",web){web=!web}}
   SoftCard{Text("Execution budget",style=androidx.compose.material3.MaterialTheme.typography.titleMedium);Slider(budget,{budget=it},valueRange=.1f..1f);Text("Tool budget ${((budget*100).toInt())}%");Slider(maxTools.toFloat(),{maxTools=it.toInt()},valueRange=4f..128f,steps=30);Text("Max tools per turn: $maxTools")}
   SoftCard{Text("Workspace actions",style=androidx.compose.material3.MaterialTheme.typography.titleMedium);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onChat){Text("Open chat")};Button(onClick=onTools){Text("Inspect tools")}}}
   AnimatedVisibility(planning){Text("Plan checkpoints: intent → context → tool selection → execution → artifact validation → response")}
  }
 }
}

@Composable private fun Setting(title:String,subtitle:String,value:Boolean,onToggle:()->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Column(Modifier.weight(1f)){Text(title);Text(subtitle,style=androidx.compose.material3.MaterialTheme.typography.bodySmall)};Switch(checked=value,onCheckedChange={onToggle()})}}
