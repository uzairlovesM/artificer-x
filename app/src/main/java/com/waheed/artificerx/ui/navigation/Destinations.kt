package com.waheed.artificerx.ui.navigation

/**
 * Central route registry for the single-Activity nav graph (Section 72:
 * Android Architecture). Every screen ARTIFICER-X ships across every
 * build phase gets a route here up front, even ones not wired with real
 * content yet, so the graph shape never needs churn later — only the
 * screen composable behind a route changes as phases land.
 *
 * Routes that take arguments use a base path constant plus a
 * `createRoute(...)` builder so call sites never hand-format route
 * strings and risk a typo breaking deep navigation.
 */
object Destinations {
    // Onboarding / cold-start
    const val ONBOARDING_WELCOME = "onboarding/welcome"
    const val ONBOARDING_PROVIDER_SETUP = "onboarding/provider_setup"
    const val ONBOARDING_PERMISSIONS_PRIMER = "onboarding/permissions_primer"

    // Core studio
    const val STUDIO = "studio"
    const val CANVAS = "studio/canvas"
    const val AGENT_CHAT = "studio/agent_chat"
    const val LAYER_PANEL = "studio/layer_panel"
    const val TOOL_PALETTE = "studio/tool_palette"
    const val LIVE_AGENT_LOG = "studio/live_agent_log"
    const val SHOW_PROCESS = "studio/show_process"
    const val SCULPT_STUDIO = "studio/sculpt"
    const val MAPS = "studio/maps"

    // Project management
    const val PROJECT_GALLERY = "projects/gallery"
    const val PROJECT_DETAIL_BASE = "projects/detail"
    const val PROJECT_DETAIL_ARG = "projectId"
    const val PROJECT_DETAIL = "$PROJECT_DETAIL_BASE/{$PROJECT_DETAIL_ARG}"

    fun projectDetailRoute(projectId: String) = "$PROJECT_DETAIL_BASE/$projectId"

    const val PROJECT_VERSION_HISTORY_BASE = "projects/version_history"
    const val PROJECT_VERSION_HISTORY = "$PROJECT_VERSION_HISTORY_BASE/{$PROJECT_DETAIL_ARG}"

    fun projectVersionHistoryRoute(projectId: String) = "$PROJECT_VERSION_HISTORY_BASE/$projectId"

    // Reference / mood / style
    const val REFERENCE_BOARD = "reference/board"
    const val MOODBOARD = "reference/moodboard"
    const val STYLE_MIXER = "reference/style_mixer"
    const val STYLE_DNA_LIBRARY = "reference/style_dna"
    const val CHARACTER_LIBRARY = "reference/character_library"

    // Export / share
    const val EXPORT_BASE = "export"
    const val EXPORT = "$EXPORT_BASE/{$PROJECT_DETAIL_ARG}"

    fun exportRoute(projectId: String) = "$EXPORT_BASE/$projectId"

    // Settings
    const val SETTINGS_ROOT = "settings/root"
    const val SETTINGS_AI_PROVIDERS = "settings/ai_providers"
    const val SETTINGS_LOCAL_MODEL = "settings/local_model"
    const val SETTINGS_ADD_PROVIDER = "settings/ai_providers/add"
    const val SETTINGS_PROVIDER_DETAIL_BASE = "settings/ai_providers/detail"
    const val SETTINGS_PROVIDER_DETAIL_ARG = "providerId"
    const val SETTINGS_PROVIDER_DETAIL = "$SETTINGS_PROVIDER_DETAIL_BASE/{$SETTINGS_PROVIDER_DETAIL_ARG}"

    fun providerDetailRoute(providerId: String) = "$SETTINGS_PROVIDER_DETAIL_BASE/$providerId"

    const val SETTINGS_QUALITY_BUDGET = "settings/quality_budget"
    const val SETTINGS_COST_VISIBILITY = "settings/cost_visibility"
    const val SETTINGS_STORAGE = "settings/storage"
    const val SETTINGS_BACKUP_RESTORE = "settings/backup_restore"
    const val SETTINGS_ACCESSIBILITY = "settings/accessibility"
    const val SETTINGS_ABOUT = "settings/about"

    // Mode-specific studio variants (Section 60-67)
    const val MODE_MANGA = "mode/manga"
    const val MODE_ANIME = "mode/anime"
    const val MODE_REALISTIC = "mode/realistic"
    const val MODE_CARTOON = "mode/cartoon"
    const val MODE_PAINTING = "mode/painting"
    const val MODE_PHOTO_EDIT = "mode/photo_edit"
    const val MODE_PRODUCT_DESIGN = "mode/product_design"
    const val MODE_STORYBOARD = "mode/storyboard"
    const val MODE_SPRITE = "mode/sprite"
    const val MODE_PIXEL = "mode/pixel"

    // Experiment / comparison (Section 160-162)
    const val EXPERIMENT_MODE = "experiment/mode"
    const val COMPARISON_VIEW_BASE = "experiment/comparison"
    const val COMPARISON_VIEW = "$COMPARISON_VIEW_BASE/{$PROJECT_DETAIL_ARG}"

    fun comparisonViewRoute(projectId: String) = "$COMPARISON_VIEW_BASE/$projectId"
}
