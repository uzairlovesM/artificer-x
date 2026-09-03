from pathlib import Path
root=Path('/mnt/data/artificer_upgrade_work')
def write(rel,text):
 p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)

write('app/src/main/java/com/waheed/artificerx/ui/screens/art/ColorStudioScreen.kt', r'''package com.waheed.artificerx.ui.screens.art

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
 var hue by remember{mutableFloatStateOf(0f)}; var sat by remember{mutableFloatStateOf(1f)}; var value by remember{mutableFloatStateOf(1f)}; val palette=remember{mutableStateListOf("#E8DCC9","#191918","#D97757","#8A6F52","#D6B48A","#6B7280","#FFFFFF")}
 val c=Color.hsv(hue,sat,value)
 Scaffold(topBar={WorkspaceTopBar("Color Studio","HSV/HSL-style controls, palettes and quick color memory",onBack)}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Column(Modifier.size(120.dp).background(c)){ };Column{Text("${(hue).toInt()}°  ${(sat*100).toInt()}%  ${(value*100).toInt()}%");Button(onClick={val hex="#%02X%02X%02X".format((c.red*255).toInt(),(c.green*255).toInt(),(c.blue*255).toInt());palette.add(hex);vm.setBrushColor(hex)}){Text("Use + Save")}}}
  Text("Hue");Slider(hue,{hue=it},valueRange=0f..360f);Text("Saturation");Slider(sat,{sat=it},valueRange=0f..1f);Text("Value");Slider(value,{value=it},valueRange=0f..1f)
  Text("Palette");LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(palette){hex->Button(onClick={vm.setBrushColor(hex)}){Text(hex)}}}
 }}
}
''')

write('app/src/main/java/com/waheed/artificerx/ui/screens/art/TextStudioScreen.kt', r'''package com.waheed.artificerx.ui.screens.art

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
''')

write('app/src/main/java/com/waheed/artificerx/ui/screens/art/ReferenceStudioScreen.kt', r'''package com.waheed.artificerx.ui.screens.art

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
fun ReferenceStudioScreen(onBack:()->Unit){val refs=remember{mutableStateListOf<Uri>()};val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){it?.let(refs::add)};var active by remember{mutableStateOf<Uri?>(null)};Scaffold(topBar={WorkspaceTopBar("Reference Studio","Pinned visual references without flattening them into the artwork",onBack)}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Button(onClick={picker.launch("image/*")}){Text("Add reference image")};active?.let{AsyncImage(model=it,contentDescription="Reference",modifier=Modifier.fillMaxSize().weight(1f))};refs.forEach{u->Button(onClick={active=u}){Text(u.lastPathSegment.orEmpty())}}}}}
''')

write('app/src/main/java/com/waheed/artificerx/core/agent/AgentRepairPlanner.kt', r'''package com.waheed.artificerx.core.agent

data class RepairPlan(val shouldRetry: Boolean, val retryDelayMs: Long, val guidance: String)

object AgentRepairPlanner {
    fun classify(error: String): RepairPlan {
        val e = error.lowercase()
        return when {
            "timeout" in e || "timed out" in e -> RepairPlan(true, 700, "Retry once with a smaller payload or narrower tool scope.")
            "not found" in e || "no layer" in e -> RepairPlan(true, 150, "Inspect current project state and refresh IDs before retrying.")
            "permission" in e -> RepairPlan(false, 0, "Ask the user to grant the required runtime or SAF permission.")
            "network" in e || "http" in e -> RepairPlan(true, 1000, "Try the next configured provider or retry with backoff.")
            "unsupported" in e -> RepairPlan(false, 0, "Choose a concrete supported tool instead of inventing a capability.")
            else -> RepairPlan(true, 350, "Retry with corrected arguments after inspecting state.")
        }
    }
}
''')

write('app/src/main/java/com/waheed/artificerx/core/agent/AgentSessionPlanner.kt', r'''package com.waheed.artificerx.core.agent

data class AgentPlanStep(val id: String, val title: String, val kind: String, val dependsOn: List<String> = emptyList())

data class AgentPlan(val intent: String, val steps: List<AgentPlanStep>, val completionCriteria: List<String>)

object AgentSessionPlanner {
    fun plan(request: String): AgentPlan {
        val lower = request.lowercase()
        val creative = listOf("draw", "paint", "illustration", "anime", "manga", "design").any(lower::contains)
        val files = listOf("zip", "file", "project", "code", "app", "folder").any(lower::contains)
        val outputs = buildList {
            add(AgentPlanStep("understand", "Understand intent", "analysis"))
            add(AgentPlanStep("context", "Load relevant project context", "context", listOf("understand")))
            if (creative) add(AgentPlanStep("compose", "Compose visual plan", "creative", listOf("context")))
            if (files) add(AgentPlanStep("materialize", "Materialize files/artifacts", "artifact", listOf(if (creative) "compose" else "context")))
            add(AgentPlanStep("verify", "Inspect result and validate outputs", "verification", listOf(if (files) "materialize" else if (creative) "compose" else "context")))
            add(AgentPlanStep("finalize", "Summarize only verified results", "final", listOf("verify")))
        }
        return AgentPlan(request.take(160), outputs, listOf("No claimed artifact exists without a successful tool result", "Errors trigger repair guidance", "Final response reflects verified state"))
    }
}
''')
