package com.waheed.artificerx.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.waheed.artificerx.ui.screens.canvas.StudioScreen
import com.waheed.artificerx.ui.screens.chat.AgentChatScreen
import com.waheed.artificerx.ui.screens.onboarding.ProviderSetupScreen
import com.waheed.artificerx.ui.screens.onboarding.WelcomeScreen
import com.waheed.artificerx.ui.screens.plugins.PluginCenterScreen
import com.waheed.artificerx.ui.screens.settings.AiProvidersSettingsScreen
import com.waheed.artificerx.ui.screens.settings.LocalModelScreen
import com.waheed.artificerx.ui.screens.settings.SettingsRootScreen
import com.waheed.artificerx.ui.screens.hybrid.HybridFeatureScreen
import com.waheed.artificerx.ui.screens.hub.ArtifactHubScreen
import com.waheed.artificerx.ui.screens.hub.ToolUniverseScreen
import com.waheed.artificerx.ui.screens.hub.DiagnosticsScreen
import com.waheed.artificerx.ui.screens.command.CommandCenterScreen
import com.waheed.artificerx.ui.screens.memory.MemoryCenterScreen
import com.waheed.artificerx.ui.screens.workflow.WorkflowLabScreen
import com.waheed.artificerx.ui.screens.models.ModelPlaygroundScreen
import com.waheed.artificerx.ui.screens.security.SecurityCenterScreen
import com.waheed.artificerx.ui.screens.search.UniversalSearchScreen
import com.waheed.artificerx.ui.screens.art.ProArtStudioScreen
import com.waheed.artificerx.ui.screens.art.BrushLabScreen
import com.waheed.artificerx.ui.screens.art.LayerLabScreen
import com.waheed.artificerx.ui.screens.art.FilterLabScreen
import com.waheed.artificerx.ui.screens.art.RulerLabScreen
import com.waheed.artificerx.ui.screens.art.AnimationLabScreen
import com.waheed.artificerx.ui.screens.art.MaterialLabScreen
import com.waheed.artificerx.ui.screens.art.MangaPageLabScreen
import com.waheed.artificerx.ui.screens.art.ColorStudioScreen
import com.waheed.artificerx.ui.screens.art.TextStudioScreen
import com.waheed.artificerx.ui.screens.art.ReferenceStudioScreen
import com.waheed.artificerx.ui.screens.ai.AgentWorkbenchScreen
import com.waheed.artificerx.ui.screens.system.PermissionsStorageScreen
import com.waheed.artificerx.ui.screens.system.WorkspaceFileSystemScreen
import com.waheed.artificerx.ui.screens.system.SystemObservatoryScreen
import com.waheed.artificerx.ui.screens.art.CustomBrushDesignerScreen
import com.waheed.artificerx.core.art.CustomBrushStore

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.android.components.ActivityComponent::class)
interface CustomBrushEntryPoint { fun store(): CustomBrushStore }

/**
 * Top-level nav graph. Every route from Destinations gets an entry here.
 * Phase 1-4 routes (onboarding, studio, chat, provider settings) point
 * to real composables. Everything from Section 60 onward (manga mode,
 * experiment mode, style mixer, etc.) is wired to functional capability screens so
 * the graph is navigable end-to-end today, and later phases only need
 * to swap the composable body — never touch routing/argument plumbing
 * again.
 *
 * Transitions use a consistent 220ms fade+slide (matches the glass-
 * morphism brand's "soft depth" feel rather than Android's default
 * hard-cut) across every destination via NavHost's shared defaults.
 */
import com.waheed.artificerx.ui.screens.system.AutomationCenterScreen
import com.waheed.artificerx.ui.screens.system.WorkspaceSearchScreen
import com.waheed.artificerx.ui.screens.ai.AgentTimelineScreen
import com.waheed.artificerx.ui.screens.system.ExtremeControlCenterScreen

@Composable
fun ArtificerXNavGraph(
    navController: NavHostController,
    startDestination: String,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        enterTransition = {
            fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 6 }
        },
        exitTransition = {
            fadeOut(tween(180))
        },
        popEnterTransition = {
            fadeIn(tween(220))
        },
        popExitTransition = {
            fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 6 }
        },
    ) {
        composable(Destinations.ONBOARDING_WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Destinations.ONBOARDING_PROVIDER_SETUP) },
            )
        }

        composable(Destinations.ONBOARDING_PROVIDER_SETUP) {
            ProviderSetupScreen(
                onProviderConfigured = {
                    navController.navigate(Destinations.STUDIO) {
                        popUpTo(Destinations.ONBOARDING_WELCOME) { inclusive = true }
                    }
                },
                onSkipForNow = {
                    navController.navigate(Destinations.STUDIO) {
                        popUpTo(Destinations.ONBOARDING_WELCOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Destinations.ONBOARDING_PERMISSIONS_PRIMER) {
            HybridFeatureScreen("Permissions & Runtime", "Permission-aware feature gateway", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) })
        }

        composable(Destinations.STUDIO) { backStackEntry ->
            val studioViewModel: com.waheed.artificerx.ui.screens.canvas.StudioViewModel =
                androidx.hilt.navigation.compose
                    .hiltViewModel(backStackEntry)
            StudioScreen(
                snackbarHostState = snackbarHostState,
                onOpenAgentChat = { navController.navigate(Destinations.AGENT_CHAT) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS_ROOT) },
                onOpenProjectGallery = { navController.navigate(Destinations.PROJECT_GALLERY) },
                onOpenExport = { projectId -> navController.navigate(Destinations.exportRoute(projectId)) },
                onOpenSculptStudio = { navController.navigate(Destinations.SCULPT_STUDIO) },
                onOpenTimelapse = { navController.navigate(Destinations.SHOW_PROCESS) },
                viewModel = studioViewModel,
            )
        }

        composable(Destinations.CANVAS) { backStackEntry ->
            val studioViewModel: com.waheed.artificerx.ui.screens.canvas.StudioViewModel =
                androidx.hilt.navigation.compose.hiltViewModel(backStackEntry)
            StudioScreen(
                snackbarHostState = snackbarHostState,
                onOpenAgentChat = { navController.navigate(Destinations.AGENT_CHAT) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS_ROOT) },
                onOpenProjectGallery = { navController.navigate(Destinations.PROJECT_GALLERY) },
                onOpenExport = { projectId -> navController.navigate(Destinations.exportRoute(projectId)) },
                onOpenSculptStudio = { navController.navigate(Destinations.SCULPT_STUDIO) },
                onOpenTimelapse = { navController.navigate(Destinations.SHOW_PROCESS) },
                viewModel = studioViewModel,
            )
        }

        composable(Destinations.AGENT_CHAT) { backStackEntry ->
            val parentEntry =
                remember(backStackEntry) {
                    navController.getBackStackEntry(Destinations.STUDIO)
                }
            val studioViewModel: com.waheed.artificerx.ui.screens.canvas.StudioViewModel =
                androidx.hilt.navigation.compose
                    .hiltViewModel(parentEntry)
            AgentChatScreen(
                onBack = { navController.popBackStack() },
                studioViewModel = studioViewModel,
            )
        }

        composable(Destinations.SCULPT_STUDIO) {
            com.waheed.artificerx.ui.screens.sculpt.SculptScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.PLUGIN_CENTER) { PluginCenterScreen(onBack = { navController.popBackStack() }) }

        composable(Destinations.DIAGNOSTICS) { DiagnosticsScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.TOOL_UNIVERSE) { ToolUniverseScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.ARTIFACT_HUB) { ArtifactHubScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.COMMAND_CENTER) {
            CommandCenterScreen(
                onBack = { navController.popBackStack() },
                onPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) },
                onTools = { navController.navigate(Destinations.TOOL_UNIVERSE) },
                onArtifacts = { navController.navigate(Destinations.ARTIFACT_HUB) },
                onMemory = { navController.navigate(Destinations.MEMORY_CENTER) },
                onWorkflow = { navController.navigate(Destinations.WORKFLOW_LAB) },
                onSecurity = { navController.navigate(Destinations.SECURITY_CENTER) },
                onSearch = { navController.navigate(Destinations.WORKSPACE_SEARCH) },
                onDiagnostics = { navController.navigate(Destinations.DIAGNOSTICS) },
                onExport = { navController.navigate(Destinations.WORKSPACE_EXPORT) },
                onImport = { navController.navigate(Destinations.WORKSPACE_IMPORT) },
                onArtStudio = { navController.navigate(Destinations.PRO_ART_STUDIO) },
                onAgentWorkbench = { navController.navigate(Destinations.AGENT_WORKBENCH) },
                onPermissions = { navController.navigate(Destinations.PERMISSIONS_STORAGE) },
                onWorkspaceFiles = { navController.navigate(Destinations.WORKSPACE_FILES) },
                onExtremeControl = { navController.navigate(Destinations.EXTREME_CONTROL_CENTER) },
            )
        }
        composable(Destinations.MEMORY_CENTER) { MemoryCenterScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.WORKFLOW_LAB) { WorkflowLabScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.MODEL_PLAYGROUND) { ModelPlaygroundScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.SECURITY_CENTER) { SecurityCenterScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.WORKSPACE_SEARCH) { UniversalSearchScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.WORKSPACE_EXPORT) { com.waheed.artificerx.ui.screens.export.WorkspaceBundleScreen(onBack = { navController.popBackStack() }) }
        composable(Destinations.WORKSPACE_IMPORT) { com.waheed.artificerx.ui.screens.importexport.WorkspaceImportScreen(onBack = { navController.popBackStack() }) }

        composable(Destinations.MAPS) {
            com.waheed.artificerx.ui.screens.maps.MapScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.PRO_ART_STUDIO) {
            val parent = remember { navController.getBackStackEntry(Destinations.STUDIO) }
            val vm: com.waheed.artificerx.ui.screens.canvas.StudioViewModel = androidx.hilt.navigation.compose.hiltViewModel(parent)
            ProArtStudioScreen(vm, onBack={navController.popBackStack()}, onBrushes={navController.navigate(Destinations.BRUSH_LAB)}, onLayers={navController.navigate(Destinations.LAYER_LAB)}, onFilters={navController.navigate(Destinations.FILTER_LAB)}, onRulers={navController.navigate(Destinations.RULER_LAB)}, onAnimation={navController.navigate(Destinations.ANIMATION_LAB)}, onMaterials={navController.navigate(Destinations.MATERIAL_LAB)}, onManga={navController.navigate(Destinations.MANGA_PAGE_LAB)}, onColor={navController.navigate(Destinations.COLOR_STUDIO)}, onText={navController.navigate(Destinations.TEXT_STUDIO)}, onReference={navController.navigate(Destinations.REFERENCE_STUDIO)})
        }
        composable(Destinations.BRUSH_LAB) { val parent=remember{navController.getBackStackEntry(Destinations.STUDIO)}; val vm:com.waheed.artificerx.ui.screens.canvas.StudioViewModel=androidx.hilt.navigation.compose.hiltViewModel(parent); BrushLabScreen(vm, { navController.popBackStack() }, { navController.navigate(Destinations.CUSTOM_BRUSH_DESIGNER) }) }
        composable(Destinations.LAYER_LAB) { val parent=remember{navController.getBackStackEntry(Destinations.STUDIO)}; val vm:com.waheed.artificerx.ui.screens.canvas.StudioViewModel=androidx.hilt.navigation.compose.hiltViewModel(parent); LayerLabScreen(vm,{navController.popBackStack()}) }
        composable(Destinations.FILTER_LAB) { val parent=remember{navController.getBackStackEntry(Destinations.STUDIO)}; val vm:com.waheed.artificerx.ui.screens.canvas.StudioViewModel=androidx.hilt.navigation.compose.hiltViewModel(parent); FilterLabScreen(vm,{navController.popBackStack()}) }
        composable(Destinations.RULER_LAB) { RulerLabScreen{navController.popBackStack()} }
        composable(Destinations.ANIMATION_LAB) { AnimationLabScreen{navController.popBackStack()} }
        composable(Destinations.MATERIAL_LAB) { MaterialLabScreen{navController.popBackStack()} }
        composable(Destinations.MANGA_PAGE_LAB) { MangaPageLabScreen{navController.popBackStack()} }
        composable(Destinations.COLOR_STUDIO) { val parent=remember{navController.getBackStackEntry(Destinations.STUDIO)}; val vm:com.waheed.artificerx.ui.screens.canvas.StudioViewModel=androidx.hilt.navigation.compose.hiltViewModel(parent); ColorStudioScreen(vm,{navController.popBackStack()}) }
        composable(Destinations.TEXT_STUDIO) { val parent=remember{navController.getBackStackEntry(Destinations.STUDIO)}; val vm:com.waheed.artificerx.ui.screens.canvas.StudioViewModel=androidx.hilt.navigation.compose.hiltViewModel(parent); TextStudioScreen(vm,{navController.popBackStack()}) }
        composable(Destinations.REFERENCE_STUDIO) { ReferenceStudioScreen{navController.popBackStack()} }
        composable(Destinations.AUTOMATION_CENTER) { AutomationCenterScreen { navController.popBackStack() } }
        composable(Destinations.WORKSPACE_SEARCH_ADVANCED) { WorkspaceSearchScreen { navController.popBackStack() } }
        composable(Destinations.AGENT_TIMELINE) { AgentTimelineScreen { navController.popBackStack() } }
        composable(Destinations.EXTREME_CONTROL_CENTER) { ExtremeControlCenterScreen(
            onBack = { navController.popBackStack() },
            onAutomation = { navController.navigate(Destinations.AUTOMATION_CENTER) },
            onSearch = { navController.navigate(Destinations.WORKSPACE_SEARCH_ADVANCED) },
            onTimeline = { navController.navigate(Destinations.AGENT_TIMELINE) },
            onPermissions = { navController.navigate(Destinations.PERMISSIONS_STORAGE) },
            onFiles = { navController.navigate(Destinations.WORKSPACE_FILES) },
            onObservatory = { navController.navigate(Destinations.SYSTEM_OBSERVATORY) },
        ) }
        composable(Destinations.SYSTEM_OBSERVATORY) { SystemObservatoryScreen { navController.popBackStack() } }
        composable(Destinations.CUSTOM_BRUSH_DESIGNER) {
            val activity = androidx.compose.ui.platform.LocalContext.current as android.app.Activity
            val ep = androidx.compose.runtime.remember { dagger.hilt.android.EntryPointAccessors.fromActivity(activity, CustomBrushEntryPoint::class.java) }
            CustomBrushDesignerScreen(ep.store(), onBack = { navController.popBackStack() })
        }
        composable(Destinations.AGENT_WORKBENCH) { AgentWorkbenchScreen({navController.popBackStack()},{navController.navigate(Destinations.AGENT_CHAT)},{navController.navigate(Destinations.TOOL_UNIVERSE)}) }
        composable(Destinations.PERMISSIONS_STORAGE) { PermissionsStorageScreen{navController.popBackStack()} }
        composable(Destinations.WORKSPACE_FILES) { WorkspaceFileSystemScreen{navController.popBackStack()} }

        composable(Destinations.LAYER_PANEL) {
            HybridFeatureScreen("Layer Command Center", "Layer state, history and destructive-operation guards", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) })
        }

        composable(Destinations.TOOL_PALETTE) {
            HybridFeatureScreen("Tool Universe", "Searchable tool families and capability routing", onBack = { navController.popBackStack() })
        }

        composable(Destinations.LIVE_AGENT_LOG) {
            HybridFeatureScreen("Live Agent Telemetry", "Execution trace, tool lifecycle and health signals", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) })
        }

        composable(Destinations.SHOW_PROCESS) { backStackEntry ->
            val parentEntry =
                remember(backStackEntry) {
                    navController.getBackStackEntry(Destinations.STUDIO)
                }
            val studioViewModel: com.waheed.artificerx.ui.screens.canvas.StudioViewModel =
                androidx.hilt.navigation.compose
                    .hiltViewModel(parentEntry)
            com.waheed.artificerx.ui.screens.canvas.TimelapseScreen(
                viewModel = studioViewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.PROJECT_GALLERY) {
            com.waheed.artificerx.ui.screens.gallery.ProjectGalleryScreen(
                onBack = { navController.popBackStack() },
                onOpenProject = { projectId ->
                    navController.navigate(Destinations.projectDetailRoute(projectId))
                },
                onCreateNewProject = {
                    navController.navigate(Destinations.STUDIO) {
                        popUpTo(Destinations.PROJECT_GALLERY) { inclusive = true }
                    }
                },
                onOpenVersionHistory = { projectId ->
                    navController.navigate(Destinations.projectVersionHistoryRoute(projectId))
                },
            )
        }

        composable(
            route = Destinations.PROJECT_DETAIL,
            arguments = listOf(navArgument(Destinations.PROJECT_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Destinations.PROJECT_DETAIL_ARG).orEmpty()
            HybridFeatureScreen("Project Detail", "Project $projectId • files, versions, artifacts and tools", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) })
        }

        composable(
            route = Destinations.PROJECT_VERSION_HISTORY,
            arguments = listOf(navArgument(Destinations.PROJECT_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Destinations.PROJECT_DETAIL_ARG).orEmpty()
            com.waheed.artificerx.ui.screens.history.VersionHistoryScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() },
                onRestoreComplete = { navController.popBackStack() },
            )
        }

        composable(Destinations.REFERENCE_BOARD) {
            HybridFeatureScreen("Reference Board", "Research, media references and project context", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) })
        }

        composable(Destinations.MOODBOARD) {
            HybridFeatureScreen("Moodboard Lab", "Reference clustering and image-generation preparation", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) })
        }

        composable(Destinations.STYLE_MIXER) {
            HybridFeatureScreen("Art Style Mixer", "Style composition, presets and image-generation controls", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) })
        }

        composable(Destinations.STYLE_DNA_LIBRARY) {
            HybridFeatureScreen("Style DNA Library", "Persistent visual language and reusable presets", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) })
        }

        composable(Destinations.CHARACTER_LIBRARY) {
            HybridFeatureScreen("Character Library", "Character assets, visual continuity and exports", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) })
        }

        composable(
            route = Destinations.EXPORT,
            arguments = listOf(navArgument(Destinations.PROJECT_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Destinations.PROJECT_DETAIL_ARG).orEmpty()
            val studioBackStackEntry =
                remember(backStackEntry) {
                    runCatching { navController.getBackStackEntry(Destinations.STUDIO) }.getOrNull()
                }
            if (studioBackStackEntry != null) {
                val studioViewModel: com.waheed.artificerx.ui.screens.canvas.StudioViewModel =
                    androidx.hilt.navigation.compose
                        .hiltViewModel(studioBackStackEntry)
                com.waheed.artificerx.ui.screens.export.ExportScreen(
                    projectId = projectId,
                    studioViewModel = studioViewModel,
                    onBack = { navController.popBackStack() },
                )
            } else {
                HybridFeatureScreen("Export Hub", "Open a project to attach live studio state; artifact/export controls remain available", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) })
            }
        }

        composable(Destinations.SETTINGS_ROOT) {
            SettingsRootScreen(
                onBack = { navController.popBackStack() },
                onOpenAiProviders = { navController.navigate(Destinations.SETTINGS_AI_PROVIDERS) },
                onOpenLocalModel = { navController.navigate(Destinations.SETTINGS_LOCAL_MODEL) },
                onOpenQualityBudget = { navController.navigate(Destinations.SETTINGS_QUALITY_BUDGET) },
                onOpenCostVisibility = { navController.navigate(Destinations.SETTINGS_COST_VISIBILITY) },
                onOpenStorage = { navController.navigate(Destinations.SETTINGS_STORAGE) },
                onOpenBackupRestore = { navController.navigate(Destinations.SETTINGS_BACKUP_RESTORE) },
                onOpenAccessibility = { navController.navigate(Destinations.SETTINGS_ACCESSIBILITY) },
                onOpenAbout = { navController.navigate(Destinations.SETTINGS_ABOUT) },
                onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) },
                onOpenDiagnostics = { navController.navigate(Destinations.DIAGNOSTICS) },
                onOpenTools = { navController.navigate(Destinations.TOOL_UNIVERSE) },
                onOpenArtifacts = { navController.navigate(Destinations.ARTIFACT_HUB) },
                onOpenCommandCenter = { navController.navigate(Destinations.COMMAND_CENTER) },
                onOpenMemory = { navController.navigate(Destinations.MEMORY_CENTER) },
                onOpenWorkflow = { navController.navigate(Destinations.WORKFLOW_LAB) },
                onOpenModelPlayground = { navController.navigate(Destinations.MODEL_PLAYGROUND) },
                onOpenSecurity = { navController.navigate(Destinations.SECURITY_CENTER) },
                onOpenWorkspaceSearch = { navController.navigate(Destinations.WORKSPACE_SEARCH) },
            )
        }

        composable(Destinations.SETTINGS_LOCAL_MODEL) {
            LocalModelScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.SETTINGS_AI_PROVIDERS) {
            AiProvidersSettingsScreen(
                onBack = { navController.popBackStack() },
                onAddProvider = { navController.navigate(Destinations.SETTINGS_ADD_PROVIDER) },
                onOpenProviderDetail = { providerId ->
                    navController.navigate(Destinations.providerDetailRoute(providerId))
                },
            )
        }

        composable(Destinations.SETTINGS_ADD_PROVIDER) {
            ProviderSetupScreen(
                onProviderConfigured = { navController.popBackStack() },
                onSkipForNow = { navController.popBackStack() },
            )
        }

        composable(
            route = Destinations.SETTINGS_PROVIDER_DETAIL,
            arguments = listOf(navArgument(Destinations.SETTINGS_PROVIDER_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString(Destinations.SETTINGS_PROVIDER_DETAIL_ARG).orEmpty()
            HybridFeatureScreen("Provider Detail", "Provider $providerId • health, models, quota and routing", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) })
        }

        composable(Destinations.SETTINGS_QUALITY_BUDGET) {
            com.waheed.artificerx.ui.screens.settings.QualityBudgetScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.SETTINGS_COST_VISIBILITY) {
            com.waheed.artificerx.ui.screens.settings.CostVisibilityScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.SETTINGS_STORAGE) {
            com.waheed.artificerx.ui.screens.settings.StorageManagementScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.SETTINGS_BACKUP_RESTORE) {
            com.waheed.artificerx.ui.screens.settings.BackupRestoreScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.SETTINGS_ACCESSIBILITY) {
            com.waheed.artificerx.ui.screens.settings.AccessibilityScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.SETTINGS_ABOUT) {
            com.waheed.artificerx.ui.screens.settings.AboutScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.MODE_MANGA) { HybridFeatureScreen("Manga Mode", "Panel-first illustration workspace", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) }) }
        composable(Destinations.MODE_ANIME) { HybridFeatureScreen("Anime Mode", "Anime illustration and generation workspace", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) }) }
        composable(Destinations.MODE_REALISTIC) { HybridFeatureScreen("Realistic Mode", "Photoreal creative workspace", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) }) }
        composable(Destinations.MODE_CARTOON) { HybridFeatureScreen("Cartoon Mode", "Shape-driven stylized workspace", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) }) }
        composable(Destinations.MODE_PAINTING) { HybridFeatureScreen("Painting Mode", "Brush, texture and canvas workspace", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) }) }
        composable(Destinations.MODE_PHOTO_EDIT) { HybridFeatureScreen("Photo Edit Mode", "Non-destructive image editing workspace", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) }) }
        composable(Destinations.MODE_PRODUCT_DESIGN) { HybridFeatureScreen("Product Design", "Visual product and UI concept workspace", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) }) }
        composable(Destinations.MODE_STORYBOARD) { HybridFeatureScreen("Storyboard Mode", "Scene, frame and export workflow", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) }) }
        composable(Destinations.MODE_SPRITE) { HybridFeatureScreen("Sprite Mode", "Sprite sheet and asset workspace", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) }) }
        composable(Destinations.MODE_PIXEL) { HybridFeatureScreen("Pixel Mode", "Pixel-perfect canvas and export tools", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) }) }

        composable(Destinations.EXPERIMENT_MODE) {
            HybridFeatureScreen("Experiment Lab", "Controlled model/tool comparisons", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) })
        }

        composable(
            route = Destinations.COMPARISON_VIEW,
            arguments = listOf(navArgument(Destinations.PROJECT_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Destinations.PROJECT_DETAIL_ARG).orEmpty()
            HybridFeatureScreen("Comparison View", "Project $projectId • outputs, models and revision comparison", onBack = { navController.popBackStack() }, onOpenPlugins = { navController.navigate(Destinations.PLUGIN_CENTER) }, onOpenToolUniverse = { navController.navigate(Destinations.TOOL_UNIVERSE) }, onOpenArtifactHub = { navController.navigate(Destinations.ARTIFACT_HUB) })
        }
    }
}

/**
 * Dev-safe stand-in for any route whose real screen hasn't landed yet.
 * Renders the brand background + a centered title so the graph is
 * always navigable and visually on-theme during incremental builds,
 * instead of a route-not-found crash.
 */
@Composable
