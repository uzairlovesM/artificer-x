from pathlib import Path
root=Path('/mnt/data/artificer_upgrade_work')
# Manifest permissions
p=root/'app/src/main/AndroidManifest.xml'; s=p.read_text()
marker='    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />'
if marker in s and 'READ_MEDIA_VIDEO' not in s:
    s=s.replace(marker, marker+'\n    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />\n    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />\n    <uses-permission android:name="android.permission.CAMERA" />')
p.write_text(s)

# Application inject + initialize workspace and maintenance
p=root/'app/src/main/java/com/waheed/artificerx/ArtificerXApp.kt'; s=p.read_text()
needle='    @Inject\n    lateinit var localInferenceEngine: com.waheed.artificerx.core.agent.LocalInferenceEngine\n'
insert=needle+'\n    @Inject\n    lateinit var workspaceFileSystem: com.waheed.artificerx.core.storage.WorkspaceFileSystem\n\n    @Inject\n    lateinit var workspaceManifestService: com.waheed.artificerx.core.storage.WorkspaceManifestService\n\n    @Inject\n    lateinit var workspaceMaintenanceScheduler: com.waheed.artificerx.core.background.WorkspaceMaintenanceScheduler\n'
if 'workspaceFileSystem' not in s: s=s.replace(needle,insert)
needle2='        backupScheduler.scheduleAutoBackup()\n'
if 'workspaceMaintenanceScheduler.schedule()' not in s: s=s.replace(needle2, needle2+'        workspaceFileSystem.ensureReady()\n        workspaceManifestService.refresh()\n        workspaceMaintenanceScheduler.schedule()\n')
p.write_text(s)

# Destinations
p=root/'app/src/main/java/com/waheed/artificerx/ui/navigation/Destinations.kt'; s=p.read_text()
insert='''\n    // Pro art / ibisPaint-inspired editor modules\n    const val PRO_ART_STUDIO = "studio/pro_art"\n    const val BRUSH_LAB = "studio/brush_lab"\n    const val LAYER_LAB = "studio/layer_lab"\n    const val FILTER_LAB = "studio/filter_lab"\n    const val RULER_LAB = "studio/ruler_lab"\n    const val ANIMATION_LAB = "studio/animation_lab"\n    const val MATERIAL_LAB = "studio/material_lab"\n    const val MANGA_PAGE_LAB = "studio/manga_page_lab"\n\n    // Runtime / intelligence control centers\n    const val AGENT_WORKBENCH = "ai/agent_workbench"\n    const val PERMISSIONS_STORAGE = "system/permissions_storage"\n    const val WORKSPACE_FILES = "system/workspace_files"\n'''
if 'PRO_ART_STUDIO' not in s: s=s.replace('    const val MAPS = "studio/maps"\n', '    const val MAPS = "studio/maps"\n'+insert)
p.write_text(s)

# Nav graph imports and routes
p=root/'app/src/main/java/com/waheed/artificerx/ui/navigation/ArtificerXNavGraph.kt'; s=p.read_text()
imports='''import com.waheed.artificerx.ui.screens.art.ProArtStudioScreen\nimport com.waheed.artificerx.ui.screens.art.BrushLabScreen\nimport com.waheed.artificerx.ui.screens.art.LayerLabScreen\nimport com.waheed.artificerx.ui.screens.art.FilterLabScreen\nimport com.waheed.artificerx.ui.screens.art.RulerLabScreen\nimport com.waheed.artificerx.ui.screens.art.AnimationLabScreen\nimport com.waheed.artificerx.ui.screens.art.MaterialLabScreen\nimport com.waheed.artificerx.ui.screens.art.MangaPageLabScreen\nimport com.waheed.artificerx.ui.screens.ai.AgentWorkbenchScreen\nimport com.waheed.artificerx.ui.screens.system.PermissionsStorageScreen\nimport com.waheed.artificerx.ui.screens.system.WorkspaceFileSystemScreen\n'''
if 'screens.art.ProArtStudioScreen' not in s:
    s=s.replace('import com.waheed.artificerx.ui.screens.search.WorkspaceSearchScreen\n', 'import com.waheed.artificerx.ui.screens.search.WorkspaceSearchScreen\n'+imports)
anchor='''        composable(Destinations.MAPS) {\n            com.waheed.artificerx.ui.screens.maps.MapScreen(\n                onBack = { navController.popBackStack() },\n            )\n        }\n'''
routes='''\n        composable(Destinations.PRO_ART_STUDIO) {\n            val parent = remember { navController.getBackStackEntry(Destinations.STUDIO) }\n            val vm: com.waheed.artificerx.ui.screens.canvas.StudioViewModel = androidx.hilt.navigation.compose.hiltViewModel(parent)\n            ProArtStudioScreen(vm, onBack={navController.popBackStack()}, onBrushes={navController.navigate(Destinations.BRUSH_LAB)}, onLayers={navController.navigate(Destinations.LAYER_LAB)}, onFilters={navController.navigate(Destinations.FILTER_LAB)}, onRulers={navController.navigate(Destinations.RULER_LAB)}, onAnimation={navController.navigate(Destinations.ANIMATION_LAB)}, onMaterials={navController.navigate(Destinations.MATERIAL_LAB)}, onManga={navController.navigate(Destinations.MANGA_PAGE_LAB)})\n        }\n        composable(Destinations.BRUSH_LAB) { val parent=remember{navController.getBackStackEntry(Destinations.STUDIO)}; val vm:com.waheed.artificerx.ui.screens.canvas.StudioViewModel=androidx.hilt.navigation.compose.hiltViewModel(parent); BrushLabScreen(vm,{navController.popBackStack()}) }\n        composable(Destinations.LAYER_LAB) { val parent=remember{navController.getBackStackEntry(Destinations.STUDIO)}; val vm:com.waheed.artificerx.ui.screens.canvas.StudioViewModel=androidx.hilt.navigation.compose.hiltViewModel(parent); LayerLabScreen(vm,{navController.popBackStack()}) }\n        composable(Destinations.FILTER_LAB) { val parent=remember{navController.getBackStackEntry(Destinations.STUDIO)}; val vm:com.waheed.artificerx.ui.screens.canvas.StudioViewModel=androidx.hilt.navigation.compose.hiltViewModel(parent); FilterLabScreen(vm,{navController.popBackStack()}) }\n        composable(Destinations.RULER_LAB) { RulerLabScreen{navController.popBackStack()} }\n        composable(Destinations.ANIMATION_LAB) { AnimationLabScreen{navController.popBackStack()} }\n        composable(Destinations.MATERIAL_LAB) { MaterialLabScreen{navController.popBackStack()} }\n        composable(Destinations.MANGA_PAGE_LAB) { MangaPageLabScreen{navController.popBackStack()} }\n        composable(Destinations.AGENT_WORKBENCH) { AgentWorkbenchScreen({navController.popBackStack()},{navController.navigate(Destinations.AGENT_CHAT)},{navController.navigate(Destinations.TOOL_UNIVERSE)}) }\n        composable(Destinations.PERMISSIONS_STORAGE) { PermissionsStorageScreen{navController.popBackStack()} }\n        composable(Destinations.WORKSPACE_FILES) { WorkspaceFileSystemScreen{navController.popBackStack()} }\n'''
if 'composable(Destinations.PRO_ART_STUDIO)' not in s: s=s.replace(anchor,anchor+routes)
p.write_text(s)

# Add routes to command center buttons by rewriting action section minimal
p=root/'app/src/main/java/com/waheed/artificerx/ui/screens/command/CommandCenterScreen.kt'; s=p.read_text()
s=s.replace('    onImport: () -> Unit,\n)', '    onImport: () -> Unit,\n    onArtStudio: () -> Unit = {},\n    onAgentWorkbench: () -> Unit = {},\n    onPermissions: () -> Unit = {},\n    onWorkspaceFiles: () -> Unit = {},\n)')
s=s.replace('            ActionButton("Import", Icons.Filled.Folder, onImport)\n', '            ActionButton("Import", Icons.Filled.Folder, onImport)\n            ActionButton("Art", Icons.Filled.Brush, onArtStudio)\n            ActionButton("AI", Icons.Filled.AutoMode, onAgentWorkbench)\n            ActionButton("Access", Icons.Filled.Security, onPermissions)\n            ActionButton("Tree", Icons.Filled.Folder, onWorkspaceFiles)\n')
# add Brush import if absent
if 'icons.filled.Brush' not in s: s=s.replace('import androidx.compose.material.icons.filled.BugReport\n', 'import androidx.compose.material.icons.filled.BugReport\nimport androidx.compose.material.icons.filled.Brush\n')
p.write_text(s)

# Wire CommandCenter in nav graph
p=root/'app/src/main/java/com/waheed/artificerx/ui/navigation/ArtificerXNavGraph.kt'; s=p.read_text()
old='''                onImport = { navController.navigate(Destinations.WORKSPACE_IMPORT) },\n            )\n'''
new='''                onImport = { navController.navigate(Destinations.WORKSPACE_IMPORT) },\n                onArtStudio = { navController.navigate(Destinations.PRO_ART_STUDIO) },\n                onAgentWorkbench = { navController.navigate(Destinations.AGENT_WORKBENCH) },\n                onPermissions = { navController.navigate(Destinations.PERMISSIONS_STORAGE) },\n                onWorkspaceFiles = { navController.navigate(Destinations.WORKSPACE_FILES) },\n            )\n'''
s=s.replace(old,new)
p.write_text(s)
