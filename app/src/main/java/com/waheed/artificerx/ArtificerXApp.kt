package com.waheed.artificerx

import com.waheed.artificerx.core.runtime.RuntimeToolCatalog
import com.waheed.artificerx.core.builtin.BuiltinRecipeCatalog
import com.waheed.artificerx.core.ai.intelligence.StrategyCatalog
import com.waheed.artificerx.runtime.capability.AdvancedCapabilityCatalog

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ARTIFICER-X application root.
 *
 * Responsibilities beyond the Hilt component graph:
 *  - Notification channel registration (render progress, agent status,
 *    quality-gate alerts, provider-quota warnings — Section 210).
 *  - Process-wide crash-safe save hook (Section 147: "Crash-Safe Save")
 *    via an uncaught exception handler that flushes any pending project
 *    state to disk BEFORE the process dies, instead of losing work.
 *  - App-level lifecycle observation (foreground/background) so the
 *    agent event loop (Section 155) can pause heavy work when backgrounded,
 *    matching Section 71 Offline-First / Section 137 Thermal Awareness.
 *  - Battery + thermal state exposure (Section 136/137: Battery & Thermal
 *    Awareness) as app-wide StateFlows the render scheduler reads before
 *    dispatching heavy tool calls.
 *  - Network connectivity monitoring (Section 71 Offline-First Architecture)
 *    so the agent knows before attempting a remote tool call whether the
 *    Reasoning Brain / Rendering Fabric is even reachable.
 *  - WorkManager custom Configuration wired to HiltWorkerFactory so
 *    background workers (auto-backup and future scheduled jobs —
 *    Section 139) get Hilt injection.
 */
@HiltAndroidApp
class ArtificerXApp :
    Application(),
    Configuration.Provider,
    DefaultLifecycleObserver {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var backupScheduler: com.waheed.artificerx.core.worker.BackupScheduler

    @Inject
    lateinit var localInferenceEngine: com.waheed.artificerx.core.agent.LocalInferenceEngine

    @Inject
    lateinit var workspaceFileSystem: com.waheed.artificerx.core.storage.WorkspaceFileSystem

    @Inject
    lateinit var workspaceManifestService: com.waheed.artificerx.core.storage.WorkspaceManifestService

    @Inject
    lateinit var workspaceMaintenanceScheduler: com.waheed.artificerx.core.background.WorkspaceMaintenanceScheduler

    @Inject
    lateinit var automationScheduler: com.waheed.artificerx.core.automation.AutomationScheduler

    @Inject
    lateinit var builtinRecipeCatalog: BuiltinRecipeCatalog

    @Inject
    lateinit var strategyCatalog: StrategyCatalog

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val advancedCapabilityCatalog = AdvancedCapabilityCatalog()

    // ── App-wide observable device/runtime state ──
    private val _isAppForegrounded = MutableStateFlow(true)
    val isAppForegrounded: StateFlow<Boolean> = _isAppForegrounded.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _isMeteredConnection = MutableStateFlow(false)
    val isMeteredConnection: StateFlow<Boolean> = _isMeteredConnection.asStateFlow()

    private val _batteryLevelPercent = MutableStateFlow(100)
    val batteryLevelPercent: StateFlow<Int> = _batteryLevelPercent.asStateFlow()

    private val _isBatteryLow = MutableStateFlow(false)
    val isBatteryLow: StateFlow<Boolean> = _isBatteryLow.asStateFlow()

    private val _isPowerSaveMode = MutableStateFlow(false)
    val isPowerSaveMode: StateFlow<Boolean> = _isPowerSaveMode.asStateFlow()

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var powerManager: PowerManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate() {
        super<Application>.onCreate()
        if (isDebugBuild()) {
            timber.log.Timber.plant(timber.log.Timber.DebugTree())
        }
        registerUncaughtExceptionHandler()
        createNotificationChannels()
        initDeviceStateMonitors()
        configureOsmdroid()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        backupScheduler.scheduleAutoBackup()
        workspaceFileSystem.ensureReady()
        RuntimeToolCatalog.init(this)
        builtinRecipeCatalog.init(this)
        strategyCatalog.init(this)
        advancedCapabilityCatalog.registerDefaults()
        workspaceManifestService.refresh()
        workspaceMaintenanceScheduler.schedule()
        automationScheduler.scheduleDaily()
        Log.i(TAG, "ARTIFICER-X process started. debug=${isDebugBuild()} capabilities=${advancedCapabilityCatalog.all().size}")
    }

    /** Section: Maps/location services. osmdroid's tile servers
     *  (OpenStreetMap.org's own, by default) reject requests with no
     *  identifying User-Agent header — every request otherwise arrives
     *  looking identical to every other unconfigured osmdroid app on
     *  earth, which OSM's tile-usage policy explicitly disallows and
     *  actively 403s. This MUST run before any osmdroid MapView is
     *  ever constructed (Configuration.getInstance() is a
     *  process-wide singleton osmdroid reads from internally on first
     *  tile request), so it lives here in Application.onCreate()
     *  rather than in MapScreen itself, where a fast navigation to
     *  the map before this ran would race it. */
    private fun configureOsmdroid() {
        val osmConfig =
            org.osmdroid.config.Configuration
                .getInstance()
        osmConfig.userAgentValue = "$packageName/${BuildConfig.VERSION_NAME}"
        osmConfig.load(this, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        osmConfig.osmdroidTileCache = java.io.File(cacheDir, "osmdroid/tiles")
        osmConfig.osmdroidBasePath = java.io.File(filesDir, "osmdroid")
    }

    // ── Crash-safe save (Section 147) ──
    // Wraps the default handler: on ANY uncaught exception, broadcast a
    // synchronous "flush now" signal through CrashSafeSaveRegistry before
    // letting the original handler terminate the process. Any active
    // ViewModel/repository holding unsaved canvas/project state registers
    // a flush callback there at creation time.
    private fun registerUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "FATAL — crash-safe save flush triggered", throwable)
                CrashSafeSaveRegistry.flushAllSynchronously()
            } catch (flushError: Throwable) {
                Log.e(TAG, "Crash-safe save flush itself failed", flushError)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_RENDER_PROGRESS,
                    "Render Progress",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Live progress while the agent generates or refines artwork"
                    setShowBadge(false)
                },
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_AGENT_STATUS,
                    "Agent Status",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Agent completed a task, needs input, or hit an error"
                },
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_QUALITY_GATE,
                    "Quality Gate Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Self-correction loop found an issue requiring your review"
                },
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PROVIDER_QUOTA,
                    "Provider Quota Warnings",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "A free-tier AI provider is near or has hit its daily quota"
                },
            )
        }
    }

    // ── Battery / Thermal / Network monitors (Section 136/137/71) ──
    private fun initDeviceStateMonitors() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        networkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isNetworkAvailable.value = true
                    updateMeteredState()
                }

                override fun onLost(network: Network) {
                    _isNetworkAvailable.value = false
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    _isNetworkAvailable.value =
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    _isMeteredConnection.value =
                        !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                }
            }

        val request =
            NetworkRequest
                .Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        updateMeteredState()
        _isPowerSaveMode.value = powerManager.isPowerSaveMode

        val batteryStatusFilter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(
            object : android.content.BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: android.content.Intent?,
                ) {
                    intent ?: return
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        val pct = (level * 100) / scale
                        _batteryLevelPercent.value = pct
                        _isBatteryLow.value = pct <= BATTERY_LOW_THRESHOLD_PERCENT
                    }
                    _isPowerSaveMode.value = powerManager.isPowerSaveMode
                }
            },
            batteryStatusFilter,
        )
    }

    private fun updateMeteredState() {
        val activeNetwork = connectivityManager.activeNetwork ?: return
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return
        _isMeteredConnection.value =
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    // ── Foreground/background lifecycle (Section 71/137) ──
    override fun onStart(owner: LifecycleOwner) {
        _isAppForegrounded.value = true
        appScope.launch { AgentEventBus.emitLifecycleEvent(isForegrounded = true) }
    }

    override fun onStop(owner: LifecycleOwner) {
        _isAppForegrounded.value = false
        appScope.launch { AgentEventBus.emitLifecycleEvent(isForegrounded = false) }
        // Section: Local Model provider — see LocalInferenceEngine.
        // unloadDueToBackgrounding's own doc for why. Guarded inside
        // the engine itself (no-op if nothing is loaded), so this is
        // always safe to call unconditionally.
        localInferenceEngine.unloadDueToBackgrounding()
    }

    // ── WorkManager + Hilt wiring ──
    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(if (isDebugBuild()) Log.DEBUG else Log.ERROR)
                .build()

    private fun isDebugBuild(): Boolean = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    companion object {
        private const val TAG = "ArtificerXApp"
        const val CHANNEL_RENDER_PROGRESS = "render_progress_channel"
        const val CHANNEL_AGENT_STATUS = "agent_status_channel"
        const val CHANNEL_QUALITY_GATE = "quality_gate_channel"
        const val CHANNEL_PROVIDER_QUOTA = "provider_quota_channel"
        private const val BATTERY_LOW_THRESHOLD_PERCENT = 20
    }

    /** Snapshot used by diagnostics and the hidden AI control plane. */
    private fun expansionCapabilityCount(): Int =
        com.waheed.artificerx.core.expansion.ExpansionRuntime.all().size

    /** Returns only failed capability checks so boot diagnostics can distinguish absence from failure. */
    private fun expansionCapabilityFailures(): List<com.waheed.artificerx.core.expansion.CapabilityCheck> =
        com.waheed.artificerx.core.expansion.ExpansionRuntime.failures()

}

/**
 * Process-wide registry that ViewModels/repositories holding unsaved
 * project/canvas state register a synchronous flush callback with.
 * Invoked from the uncaught exception handler right before the process
 * dies (Section 147: Crash-Safe Save). Deliberately not a Hilt-injected
 * singleton — must be reachable even if the DI graph itself is what
 * crashed, so it's a plain thread-safe object.
 */
object CrashSafeSaveRegistry {
    private val callbacks = java.util.concurrent.CopyOnWriteArrayList<() -> Unit>()

    fun register(flush: () -> Unit) {
        callbacks.add(flush)
    }

    fun unregister(flush: () -> Unit) {
        callbacks.remove(flush)
    }

    fun flushAllSynchronously() {
        callbacks.forEach { callback ->
            runCatching { callback() }
        }
    }
}

/**
 * Minimal process-wide event bus (Section 154: Event Bus) for lifecycle
 * signals that the Agent Event Loop (Section 155) needs regardless of
 * which screen is currently composed — e.g. pausing heavy tool-call
 * dispatch when the app is backgrounded. The full multi-topic event bus
 * (agent state transitions, tool results, quality-gate events) lives in
 * core.agent and is wired via Hilt; this lightweight singleton only
 * carries app-lifecycle signals that must exist before DI is guaranteed
 * ready.
 */
object AgentEventBus {
    private val _lifecycleEvents = MutableStateFlow(true)
    val lifecycleEvents: StateFlow<Boolean> = _lifecycleEvents.asStateFlow()

    fun emitLifecycleEvent(isForegrounded: Boolean) {
        _lifecycleEvents.value = isForegrounded
    }
}
