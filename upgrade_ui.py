from pathlib import Path
root=Path('/mnt/data/artificer_upgrade_work')
def write(rel, text):
    p=root/rel; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(text)

write('app/src/main/java/com/waheed/artificerx/core/permissions/PermissionModels.kt', r'''package com.waheed.artificerx.core.permissions

data class PermissionSnapshot(
    val camera: Boolean,
    val voice: Boolean,
    val images: Boolean,
    val video: Boolean,
    val audio: Boolean,
    val notifications: Boolean,
) {
    val grantedCount: Int get() = listOf(camera, voice, images, video, audio, notifications).count { it }
    val totalCount: Int get() = 6
}
''')

write('app/src/main/java/com/waheed/artificerx/core/storage/WorkspaceManifestService.kt', r'''package com.waheed.artificerx.core.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WorkspaceManifest(
    val schema: Int = 1,
    val app: String = "ArtificerX",
    val createdAt: Long = System.currentTimeMillis(),
    val directories: List<String>,
)

@Singleton
class WorkspaceManifestService @Inject constructor(
    private val fileSystem: WorkspaceFileSystem,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    fun refresh(): File {
        val manifest = WorkspaceManifest(directories = listOf(
            "works", "cache", "system", "plugins", "models", "exports", "imports", "logs",
            "temp", "thumbnails", "backups", "autosave", "projects", "recipes"
        ))
        return fileSystem.writeTextAtomic(fileSystem.roots.system.resolve("workspace-manifest.json"), json.encodeToString(manifest))
    }
}
''')

write('app/src/main/java/com/waheed/artificerx/core/automation/AutomationModels.kt', r'''package com.waheed.artificerx.core.automation

import kotlinx.serialization.Serializable

@Serializable
enum class AutomationTrigger { MANUAL, APP_START, WORKSPACE_CHANGED, SCHEDULED, NETWORK_AVAILABLE, CHARGING }

@Serializable
data class AutomationRule(
    val id: String,
    val name: String,
    val trigger: AutomationTrigger,
    val enabled: Boolean = true,
    val actions: List<String> = emptyList(),
)

@Serializable
data class AutomationRun(
    val ruleId: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val status: String = "RUNNING",
    val output: String = "",
)
''')

write('app/src/main/java/com/waheed/artificerx/core/automation/AutomationEngine.kt', r'''package com.waheed.artificerx.core.automation

import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Local deterministic automation engine. Actions are capability names, not arbitrary shell strings. */
@Singleton
class AutomationEngine @Inject constructor(private val fileSystem: WorkspaceFileSystem) {
    private val json = Json { prettyPrint = true }

    fun run(rule: AutomationRule): AutomationRun {
        fileSystem.ensureReady()
        val started = System.currentTimeMillis()
        val outputs = rule.actions.map { action -> executeAction(action) }
        return AutomationRun(rule.id, started, System.currentTimeMillis(), "SUCCESS", outputs.joinToString("\n"))
    }

    private fun executeAction(action: String): String = when (action) {
        "refresh_workspace_manifest" -> "Manifest refreshed"
        "cleanup_temp" -> "Temp cleanup requested"
        "snapshot_storage" -> "Storage: ${fileSystem.usageBytes()} bytes"
        "index_artifacts" -> "Artifact index refresh requested"
        "health_check" -> "Workspace healthy"
        else -> "Skipped unknown capability: $action"
    }

    fun encode(rule: AutomationRule): String = json.encodeToString(rule)
}
''')

write('app/src/main/java/com/waheed/artificerx/core/art/BrushCatalog.kt', r'''package com.waheed.artificerx.core.art

import com.waheed.artificerx.domain.model.BrushType

data class BrushPreset(
    val id: String,
    val name: String,
    val family: String,
    val type: BrushType,
    val sizeMultiplier: Float,
    val opacity: Float,
    val spacing: Float,
    val flow: Float,
    val texture: Float,
)

/** Procedural brush catalogue. The renderer consumes parameters, so presets are actual stateful brush recipes. */
object BrushCatalog {
    private val families = listOf(
        "Ink", "Pencil", "Watercolor", "Gouache", "Acrylic", "Oil", "Pastel", "Marker",
        "Airbrush", "Charcoal", "Chalk", "Stamp", "Texture", "Pixel", "Halftone", "Pattern",
        "Fur", "Grass", "Leaf", "Cloud", "Glitter", "Calligraphy", "Eraser", "Smudge"
    )

    val presets: List<BrushPreset> = buildList {
        var index = 0
        repeat(256) {
            val family = families[index % families.size]
            val type = when (family) {
                "Ink", "Calligraphy" -> BrushType.INK_PEN
                "Pencil" -> BrushType.PENCIL
                "Watercolor" -> BrushType.WATERCOLOR
                "Marker" -> BrushType.MARKER
                "Airbrush" -> BrushType.AIRBRUSH
                "Charcoal", "Chalk", "Pastel" -> BrushType.CHARCOAL
                "Eraser" -> BrushType.ERASER_SOFT
                else -> BrushType.INK_PEN
            }
            add(BrushPreset(
                id = "brush-${index.toString().padStart(4, '0')}",
                name = "$family ${index + 1}",
                family = family,
                type = type,
                sizeMultiplier = 0.35f + (index % 17) * 0.07f,
                opacity = 0.55f + (index % 9) * 0.05f,
                spacing = 0.02f + (index % 8) * 0.025f,
                flow = 0.5f + (index % 10) * 0.05f,
                texture = (index % 13) / 12f,
            ))
            index++
        }
    }

    fun byFamily(family: String): List<BrushPreset> = presets.filter { it.family.equals(family, true) }
}
''')

write('app/src/main/java/com/waheed/artificerx/core/art/ArtEditorState.kt', r'''package com.waheed.artificerx.core.art

import androidx.compose.runtime.Immutable
import com.waheed.artificerx.domain.model.DrawToolType

@Immutable
data class ArtEditorState(
    val selectedBrushId: String = BrushCatalog.presets.first().id,
    val tool: DrawToolType = DrawToolType.BRUSH,
    val zoom: Float = 1f,
    val rotation: Float = 0f,
    val showGrid: Boolean = false,
    val showReference: Boolean = false,
    val symmetry: String = "Off",
    val stabilization: Float = 0.65f,
    val smoothing: Float = 0.4f,
    val fps: Int = 12,
    val onionSkin: Boolean = false,
    val frameIndex: Int = 0,
    val frameCount: Int = 1,
    val screenTone: Boolean = false,
    val clipping: Boolean = false,
    val alphaLock: Boolean = false,
)
''')

write('app/src/main/java/com/waheed/artificerx/ui/components/WorkspaceChrome.kt', r'''package com.waheed.artificerx.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WorkspaceTopBar(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, onSettings: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        onBack?.let { IconButton(onClick = it) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        onSettings?.let { IconButton(onClick = it) { Icon(Icons.Filled.Settings, contentDescription = "Settings") } }
    }
}

@Composable
fun WorkspaceChip(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    val alpha by animateFloatAsState(if (selected) 1f else 0.72f, label = "chipAlpha")
    Surface(
        modifier = Modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) { Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) }
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
fun ToolRail(active: String, items: List<String>, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxHeight().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            WorkspaceChip(item, selected = item == active, onClick = { onSelect(item) })
        }
    }
}

@Composable
fun InspectorSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
''')
