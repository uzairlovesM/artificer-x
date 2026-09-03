from pathlib import Path
root=Path('/mnt/data/artificer_upgrade_work')
# StudioViewModel text wrapper
p=root/'app/src/main/java/com/waheed/artificerx/ui/screens/canvas/StudioViewModel.kt'; s=p.read_text()
needle='        fun applyActiveFilter(filterType: String, intensity: Float = 1f) {'
insert='''        fun addTextLayer(text: String, fontSizePx: Float = 48f, colorHex: String = "#191918", bold: Boolean = false) {
            if (text.isBlank()) return
            val active = _state.value.activeLayerId ?: return
            val layer = _state.value.layers.firstOrNull { it.id == active } ?: return
            if (layer.isLocked) return
            bitmapStore.ensureLayer(active, _state.value.canvasWidthPx, _state.value.canvasHeightPx)
            bitmapStore.pushUndoSnapshot()
            if (compositor.addText(active, text, 80f, 140f, fontSizePx, colorHex, bold)) recomposite()
        }\n\n'''
if 'fun addTextLayer(' not in s:s=s.replace(needle,insert+needle)
p.write_text(s)

# Fix Workspace file screen cleanly
p=root/'app/src/main/java/com/waheed/artificerx/ui/screens/system/WorkspaceFileSystemScreen.kt'
p.write_text('''package com.waheed.artificerx.ui.screens.system\n\nimport androidx.compose.foundation.layout.Arrangement\nimport androidx.compose.foundation.layout.Column\nimport androidx.compose.foundation.layout.fillMaxSize\nimport androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.lazy.items\nimport androidx.compose.material3.Button\nimport androidx.compose.material3.Text\nimport androidx.compose.material3.Scaffold\nimport androidx.compose.runtime.Composable\nimport androidx.compose.ui.Modifier\nimport androidx.compose.ui.unit.dp\nimport com.waheed.artificerx.core.storage.WorkspaceFileSystem\nimport com.waheed.artificerx.ui.components.WorkspaceTopBar\nimport dagger.hilt.android.EntryPointAccessors\nimport dagger.hilt.EntryPoint\nimport dagger.hilt.InstallIn\nimport dagger.hilt.android.components.ActivityComponent\n\n@EntryPoint\n@InstallIn(ActivityComponent::class)\ninterface WorkspaceFsEntryPoint { fun fs(): WorkspaceFileSystem }\n\n@Composable\nfun WorkspaceFileSystemScreen(onBack:()->Unit){\n    val context=androidx.compose.ui.platform.LocalContext.current\n    val activity=context as android.app.Activity\n    val ep=EntryPointAccessors.fromActivity(activity,WorkspaceFsEntryPoint::class.java)\n    val fs=ep.fs()\n    val roots=listOf("works" to fs.roots.works,"cache" to fs.roots.cache,"system" to fs.roots.system,"plugins" to fs.roots.plugins,"models" to fs.roots.models,"exports" to fs.roots.exports,"imports" to fs.roots.imports,"logs" to fs.roots.logs,"temp" to fs.roots.temp,"thumbnails" to fs.roots.thumbnails,"backups" to fs.roots.backups,"autosave" to fs.roots.autosave,"projects" to fs.roots.projects,"recipes" to fs.roots.recipes)\n    Scaffold(topBar={WorkspaceTopBar("Workspace Files","Inspect the real on-device Artificer-X data tree",onBack)}){pad->\n        Column(Modifier.fillMaxSize().padding(pad).padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){\n            Button(onClick={fs.ensureReady()}){Text("Initialize / refresh")}; Text("${fs.roots.root.absolutePath}\\nTotal ${fs.usageBytes()} bytes")\n            LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(roots){(n,f)->Column{Text(n);Text(f.absolutePath);Text("${f.length()} bytes • ${f.walkTopDown().count{it.isFile}} files",style=androidx.compose.material3.MaterialTheme.typography.bodySmall)}}}\n        }\n    }\n}\n''')

# permissions screen fix all-files action + remove unused imports
p=root/'app/src/main/java/com/waheed/artificerx/ui/screens/system/PermissionsStorageScreen.kt'; s=p.read_text().replace('OutlinedButton(onClick={runCatching{context.startActivity(PermissionManager.manageAllFilesIntent(context))}.getOrNull() as Unit}){Text("Open all-files settings") }','OutlinedButton(onClick={runCatching{context.startActivity(PermissionManager.manageAllFilesIntent(context))}}){Text("Open all-files settings") }')
p.write_text(s)

# scheduler qualifier
p=root/'app/src/main/java/com/waheed/artificerx/core/background/WorkspaceMaintenanceScheduler.kt'; s=p.read_text().replace('import android.content.Context\n','import android.content.Context\nimport dagger.hilt.android.qualifiers.ApplicationContext\n').replace('class WorkspaceMaintenanceScheduler @Inject constructor(private val context: Context)', 'class WorkspaceMaintenanceScheduler @Inject constructor(@ApplicationContext private val context: Context)')
p.write_text(s)

# routes
p=root/'app/src/main/java/com/waheed/artificerx/ui/navigation/Destinations.kt'; s=p.read_text(); marker='    const val MANGA_PAGE_LAB = "studio/manga_page_lab"\n'; add='''    const val COLOR_STUDIO = "studio/color_studio"\n    const val TEXT_STUDIO = "studio/text_studio"\n    const val REFERENCE_STUDIO = "studio/reference_studio"\n'''
if 'COLOR_STUDIO' not in s:s=s.replace(marker,marker+add)
p.write_text(s)

# Nav imports and routes
p=root/'app/src/main/java/com/waheed/artificerx/ui/navigation/ArtificerXNavGraph.kt'; s=p.read_text()
if 'ColorStudioScreen' not in s:s=s.replace('import com.waheed.artificerx.ui.screens.art.MangaPageLabScreen\n','import com.waheed.artificerx.ui.screens.art.MangaPageLabScreen\nimport com.waheed.artificerx.ui.screens.art.ColorStudioScreen\nimport com.waheed.artificerx.ui.screens.art.TextStudioScreen\nimport com.waheed.artificerx.ui.screens.art.ReferenceStudioScreen\n')
marker='        composable(Destinations.MANGA_PAGE_LAB) { MangaPageLabScreen{navController.popBackStack()} }\n'
extra='''        composable(Destinations.COLOR_STUDIO) { val parent=remember{navController.getBackStackEntry(Destinations.STUDIO)}; val vm:com.waheed.artificerx.ui.screens.canvas.StudioViewModel=androidx.hilt.navigation.compose.hiltViewModel(parent); ColorStudioScreen(vm,{navController.popBackStack()}) }\n        composable(Destinations.TEXT_STUDIO) { val parent=remember{navController.getBackStackEntry(Destinations.STUDIO)}; val vm:com.waheed.artificerx.ui.screens.canvas.StudioViewModel=androidx.hilt.navigation.compose.hiltViewModel(parent); TextStudioScreen(vm,{navController.popBackStack()}) }\n        composable(Destinations.REFERENCE_STUDIO) { ReferenceStudioScreen{navController.popBackStack()} }\n'''
if 'composable(Destinations.COLOR_STUDIO)' not in s:s=s.replace(marker,marker+extra)
p.write_text(s)

# ProArt add color/text/reference buttons
p=root/'app/src/main/java/com/waheed/artificerx/ui/screens/art/ProArtStudioScreen.kt'; s=p.read_text()
s=s.replace('onManga: () -> Unit)', 'onManga: () -> Unit, onColor: () -> Unit = {}, onText: () -> Unit = {}, onReference: () -> Unit = {})')
s=s.replace('ToolAction("Anim", Icons.Filled.PlayArrow, onAnimation)', 'ToolAction("Anim", Icons.Filled.PlayArrow, onAnimation)\n                    ToolAction("Color", Icons.Filled.Palette, onColor)\n                    ToolAction("Text", Icons.Filled.Check, onText)\n                    ToolAction("Ref", Icons.Filled.Settings, onReference)')
p.write_text(s)

# Wire callbacks
p=root/'app/src/main/java/com/waheed/artificerx/ui/navigation/ArtificerXNavGraph.kt'; s=p.read_text()
s=s.replace('onMaterials={navController.navigate(Destinations.MATERIAL_LAB)}, onManga={navController.navigate(Destinations.MANGA_PAGE_LAB)})', 'onMaterials={navController.navigate(Destinations.MATERIAL_LAB)}, onManga={navController.navigate(Destinations.MANGA_PAGE_LAB)}, onColor={navController.navigate(Destinations.COLOR_STUDIO)}, onText={navController.navigate(Destinations.TEXT_STUDIO)}, onReference={navController.navigate(Destinations.REFERENCE_STUDIO)})')
p.write_text(s)

# New Claude-like-ish palette: keep symbol names so all old screens remain wired
p=root/'app/src/main/java/com/waheed/artificerx/ui/theme/Color.kt'; s=p.read_text()
repls={'0xFFFFD700':'0xFFD97757','0xFFB8960A':'0xFFA4513A','0xFF3A2E00':'0xFF3A241F','0xFFF2C94C':'0xFFE3A389','0xFF1A0A2E':'0xFF1F1D1A','0xFF241238':'0xFF272522','0xFF2E1846':'0xFF302D28','0xFF3A2054':'0xFF39352F','0xFF7B4FE0':'0xFF9A6B5B','0xFF5A3AA8':'0xFF765146'}
for a,b in repls.items():s=s.replace(a,b)
p.write_text(s)

# Theme scheme overrides for neutral editorial UI
p=root/'app/src/main/java/com/waheed/artificerx/ui/theme/Theme.kt'; s=p.read_text()
s=s.replace('primary = GoldPrimary,','primary = GoldPrimary,').replace('background = Color(0xFFF8F6FC),','background = Color(0xFFF6F2EC),').replace('onBackground = Color(0xFF1B1720),','onBackground = Color(0xFF1F1D1A),').replace('surface = Color(0xFFFFFFFF),','surface = Color(0xFFFCFAF7),').replace('onSurface = Color(0xFF1B1720),','onSurface = Color(0xFF1F1D1A),').replace('surfaceVariant = Color(0xFFEDE7F3),','surfaceVariant = Color(0xFFECE6DE),')
s=s.replace('backgroundWash =\n        Brush.verticalGradient(\n            colors = listOf(PurpleBase00, Color(0xFF120820), PurpleBase00),\n        )','backgroundWash =\n        Brush.verticalGradient(\n            colors = listOf(PurpleBase00, Color(0xFF171512), PurpleBase00),\n        )')
p.write_text(s)
