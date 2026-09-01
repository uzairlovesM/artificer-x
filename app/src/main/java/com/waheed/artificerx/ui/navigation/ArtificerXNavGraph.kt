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
import com.waheed.artificerx.ui.screens.settings.AiProvidersSettingsScreen
import com.waheed.artificerx.ui.screens.settings.LocalModelScreen
import com.waheed.artificerx.ui.screens.settings.SettingsRootScreen

/**
 * Top-level nav graph. Every route from Destinations gets an entry here.
 * Phase 1-4 routes (onboarding, studio, chat, provider settings) point
 * to real composables. Everything from Section 60 onward (manga mode,
 * experiment mode, style mixer, etc.) is wired to PlaceholderScreen so
 * the graph is navigable end-to-end today, and later phases only need
 * to swap the composable body — never touch routing/argument plumbing
 * again.
 *
 * Transitions use a consistent 220ms fade+slide (matches the glass-
 * morphism brand's "soft depth" feel rather than Android's default
 * hard-cut) across every destination via NavHost's shared defaults.
 */
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
            PlaceholderScreen(title = "Permissions Primer")
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

        composable(Destinations.MAPS) {
            com.waheed.artificerx.ui.screens.maps.MapScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.LAYER_PANEL) {
            PlaceholderScreen(title = "Layer Panel")
        }

        composable(Destinations.TOOL_PALETTE) {
            PlaceholderScreen(title = "Tool Palette")
        }

        composable(Destinations.LIVE_AGENT_LOG) {
            PlaceholderScreen(title = "Live Agent Log")
        }

        composable(Destinations.SHOW_PROCESS) {
            PlaceholderScreen(title = "Show Process")
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
            PlaceholderScreen(title = "Project Detail: $projectId")
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
            PlaceholderScreen(title = "Reference Board")
        }

        composable(Destinations.MOODBOARD) {
            PlaceholderScreen(title = "Moodboard to Art")
        }

        composable(Destinations.STYLE_MIXER) {
            PlaceholderScreen(title = "Art Style Mixer")
        }

        composable(Destinations.STYLE_DNA_LIBRARY) {
            PlaceholderScreen(title = "Style DNA Library")
        }

        composable(Destinations.CHARACTER_LIBRARY) {
            PlaceholderScreen(title = "Character Library")
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
                PlaceholderScreen(title = "Export unavailable — open a project from Studio first")
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
            PlaceholderScreen(title = "Provider Detail: $providerId")
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

        composable(Destinations.MODE_MANGA) { PlaceholderScreen(title = "Manga Mode") }
        composable(Destinations.MODE_ANIME) { PlaceholderScreen(title = "Anime Mode") }
        composable(Destinations.MODE_REALISTIC) { PlaceholderScreen(title = "Realistic Mode") }
        composable(Destinations.MODE_CARTOON) { PlaceholderScreen(title = "Cartoon Mode") }
        composable(Destinations.MODE_PAINTING) { PlaceholderScreen(title = "Painting Mode") }
        composable(Destinations.MODE_PHOTO_EDIT) { PlaceholderScreen(title = "Photo Edit Mode") }
        composable(Destinations.MODE_PRODUCT_DESIGN) { PlaceholderScreen(title = "Product/Design Mode") }
        composable(Destinations.MODE_STORYBOARD) { PlaceholderScreen(title = "Storyboard Mode") }
        composable(Destinations.MODE_SPRITE) { PlaceholderScreen(title = "Sprite Mode") }
        composable(Destinations.MODE_PIXEL) { PlaceholderScreen(title = "Pixel Mode") }

        composable(Destinations.EXPERIMENT_MODE) {
            PlaceholderScreen(title = "Experiment Mode")
        }

        composable(
            route = Destinations.COMPARISON_VIEW,
            arguments = listOf(navArgument(Destinations.PROJECT_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Destinations.PROJECT_DETAIL_ARG).orEmpty()
            PlaceholderScreen(title = "Comparison View: $projectId")
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
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Coming in a later build phase",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
