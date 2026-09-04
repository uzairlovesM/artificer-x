package com.waheed.artificerx

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.waheed.artificerx.ui.navigation.ArtificerXNavGraph
import com.waheed.artificerx.ui.theme.ArtificerXTheme
import com.waheed.artificerx.runtime.capability.AdvancedCapabilityCatalog
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-Activity host for the entire app (Section 72: Android Architecture —
 * single-activity, Compose-only navigation). Owns:
 *  - Native SplashScreen (Android 12+ API) with a held condition so the
 *    splash doesn't dismiss until the initial DI-backed cold-state check
 *    (does the user have zero providers configured? Section 211) resolves.
 *  - Edge-to-edge rendering (matches the dark-luxury-glassmorphism theme
 *    bleeding under system bars).
 *  - Runtime permission orchestration for every permission declared in
 *    the manifest (RECORD_AUDIO for voice commands — Section 116;
 *    READ_MEDIA_IMAGES for reference images — Section 20/118-119;
 *    POST_NOTIFICATIONS for render-progress/agent-status alerts) —
 *    requested lazily per-feature at point of use, NEVER all at cold
 *    start, per Section 207.4's Play-compliance principle even though
 *    this is a personal build.
 *  - Top-level CompositionLocal exposing device-state flows (foreground,
 *    battery, network, thermal) from ArtificerXApp down to any Composable
 *    that needs to gate heavy agent actions on them.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val startupViewModel: StartupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash frame until first-run/provider-config check resolves
        // (Section 211: zero-provider first-run state must never flash an
        // empty/broken canvas before we know which screen to land on).
        splash.setKeepOnScreenCondition { !startupViewModel.isReady.value }

        enableEdgeToEdge()

        setContent {
            val app = application as ArtificerXApp

            val isForegrounded by app.isAppForegrounded.collectAsStateWithLifecycle()
            val isNetworkAvailable by app.isNetworkAvailable.collectAsStateWithLifecycle()
            val isMetered by app.isMeteredConnection.collectAsStateWithLifecycle()
            val batteryLevel by app.batteryLevelPercent.collectAsStateWithLifecycle()
            val isBatteryLow by app.isBatteryLow.collectAsStateWithLifecycle()
            val isPowerSaveMode by app.isPowerSaveMode.collectAsStateWithLifecycle()

            val deviceState =
                remember(
                    isForegrounded,
                    isNetworkAvailable,
                    isMetered,
                    batteryLevel,
                    isBatteryLow,
                    isPowerSaveMode,
                ) {
                    DeviceRuntimeState(
                        isForegrounded = isForegrounded,
                        isNetworkAvailable = isNetworkAvailable,
                        isMeteredConnection = isMetered,
                        batteryLevelPercent = batteryLevel,
                        isBatteryLow = isBatteryLow,
                        isPowerSaveMode = isPowerSaveMode,
                    )
                }

            CompositionLocalProvider(LocalDeviceRuntimeState provides deviceState) {
                ArtificerXTheme {
                    ArtificerXRoot(startupViewModel = startupViewModel)
                }
            }
        }
    }

    /** Expansion surface health is observable without exposing the AI terminal to the UI user. */
    private fun expansionRuntimeSummary(): Map<String, Int> =
        com.waheed.artificerx.core.expansion.ExpansionRuntime.summary()

    /** Keeps capability discovery observable to diagnostics without exposing the AI terminal UI. */
    private fun capabilityRuntimeSnapshot(): Map<String, Any> {
        val catalog = AdvancedCapabilityCatalog().apply { registerDefaults() }
        return mapOf(
            "capabilities" to catalog.all().size,
            "androidApi" to Build.VERSION.SDK_INT,
            "process" to packageName,
            "foreground" to true
        )
    }

}

/**
 * Root Composable: hosts the Scaffold shell (top-level Snackbar host for
 * agent errors / provider-quota warnings — Section 210), the runtime
 * permission gate, and the nav graph. Kept as a single composition root
 * so permission requests and global snackbars don't need to be threaded
 * through every screen individually.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ArtificerXRoot(startupViewModel: StartupViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val isReady by startupViewModel.isReady.collectAsStateWithLifecycle()
    val hasAnyProviderConfigured by startupViewModel.hasAnyProviderConfigured.collectAsStateWithLifecycle()

    // POST_NOTIFICATIONS (Android 13+) is the one permission in this
    // list that has no other trigger point anywhere in the app —
    // voice input's RECORD_AUDIO and the image picker's Photo-Picker
    // API each request what they need at their own point of use
    // (SpeechRecognizer prompts automatically; ActivityResultContracts.
    // GetContent()/PickVisualMedia need no runtime permission at all
    // on modern Android). Without requesting POST_NOTIFICATIONS
    // somewhere, ArtificerXApp's render-progress/agent-status
    // notification channels would silently never actually post
    // anything on API 33+ — this was previously built (state
    // constructed, passed down through the nav graph) but never
    // actually launched anywhere, making the whole permission flow
    // dead weight. Requesting it once, here, right after the splash
    // gate releases, is the single real use this list has.
    val notificationPermissionState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            com.google.accompanist.permissions
                .rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            null
        }

    LaunchedEffect(Unit) {
        startupViewModel.checkProviderConfiguration()
    }

    LaunchedEffect(isReady, notificationPermissionState) {
        if (isReady &&
            notificationPermissionState != null &&
            notificationPermissionState.status != com.google.accompanist.permissions.PermissionStatus.Granted
        ) {
            notificationPermissionState.launchPermissionRequest()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        AnimatedVisibility(visible = isReady) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    ArtificerXNavGraph(
                        navController = navController,
                        startDestination =
                            if (hasAnyProviderConfigured) {
                                com.waheed.artificerx.ui.navigation.Destinations.STUDIO
                            } else {
                                com.waheed.artificerx.ui.navigation.Destinations.ONBOARDING_PROVIDER_SETUP
                            },
                        contentPadding = innerPadding,
                        snackbarHostState = snackbarHostState,
                    )
                }
            }
        }
    }
}

/**
 * Device-wide runtime signals every screen/agent-action gate can read
 * without threading params through the nav graph. Backed by
 * ArtificerXApp's monitors (battery, thermal-proxy via power-save mode,
 * network, foreground state) — Section 136/137/71.
 */
data class DeviceRuntimeState(
    val isForegrounded: Boolean,
    val isNetworkAvailable: Boolean,
    val isMeteredConnection: Boolean,
    val batteryLevelPercent: Int,
    val isBatteryLow: Boolean,
    val isPowerSaveMode: Boolean,
) {
    /** Section 84/135/136/137: should the agent avoid dispatching new
     *  heavy remote tool calls right now? */
    val shouldThrottleHeavyWork: Boolean
        get() = isBatteryLow || isPowerSaveMode || !isNetworkAvailable
}

val LocalDeviceRuntimeState =
    compositionLocalOf {
        DeviceRuntimeState(
            isForegrounded = true,
            isNetworkAvailable = true,
            isMeteredConnection = false,
            batteryLevelPercent = 100,
            isBatteryLow = false,
            isPowerSaveMode = false,
        )
    }

/**
 * Cold-start gate ViewModel. Resolves two things before the splash
 * screen releases: (1) whether at least one AI provider is configured
 * (Section 211 — zero-provider first-run state must route to onboarding,
 * not a broken canvas), and (2) that DI/DB warm-init has completed so
 * the first composed screen never race-conditions against an unready
 * repository.
 */
@HiltViewModel
class StartupViewModel
    @Inject
    constructor(
        private val providerConfigRepository: com.waheed.artificerx.data.repository.ProviderConfigRepository,
    ) : ViewModel() {
        private val _isReady = MutableStateFlow(false)
        val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

        private val _hasAnyProviderConfigured = MutableStateFlow(false)
        val hasAnyProviderConfigured: StateFlow<Boolean> = _hasAnyProviderConfigured.asStateFlow()

        fun checkProviderConfiguration() {
            viewModelScope.launch {
                val hasProvider =
                    runCatching {
                        providerConfigRepository.hasAnyProviderConfigured()
                    }.getOrDefault(false)
                _hasAnyProviderConfigured.value = hasProvider
                _isReady.value = true
            }
        }
    }
