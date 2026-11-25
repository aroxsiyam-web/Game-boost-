package com.example.booster.system

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Choreographer
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.booster.data.BackgroundAppRepository
import com.example.booster.data.GameProfileRepository
import com.example.booster.model.GameProfile
import com.example.booster.model.OverlayBoostMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class OverlayRuntimeStatus(
    val isServiceConnected: Boolean = false,
    val isOverlayVisible: Boolean = false,
    val foregroundPackage: String? = null,
    val isKnownGameForeground: Boolean = false,
    val measuredFps: Int = 0,
    val frameTimeMs: Float = 0f,
    val activeBoostMode: OverlayBoostMode = OverlayBoostMode.BALANCED,
)

class FpsOverlayAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private lateinit var profileRepo: GameProfileRepository
    private lateinit var backgroundAppRepo: BackgroundAppRepository
    private lateinit var shizukuManager: ShizukuManager
    private lateinit var tweaksController: DeviceTweaksController

    private var rootContainer: LinearLayout? = null
    private var statusDotView: View? = null
    private var fpsTextView: TextView? = null
    private var metaTextView: TextView? = null
    private var modeBadgeView: TextView? = null
    private var modeDrawerView: LinearLayout? = null
    private var modeButtons: Map<OverlayBoostMode, TextView> = emptyMap()
    private var layoutParams: WindowManager.LayoutParams? = null

    private var currentBoostMode = OverlayBoostMode.BALANCED
    private var isDrawerExpanded = false
    private var choreographerRunning = false
    private var windowStartNs = 0L
    private var lastFrameNs = 0L
    private var frameCount = 0
    private var accumulatedFrameMs = 0f

    private val knownGamePackages: Set<String> by lazy {
        GameProfile.defaults().flatMap { it.packageCandidates }.toSet()
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!choreographerRunning) return

            if (windowStartNs == 0L) {
                windowStartNs = frameTimeNanos
                lastFrameNs = frameTimeNanos
            } else {
                val deltaMs = (frameTimeNanos - lastFrameNs) / 1_000_000f
                lastFrameNs = frameTimeNanos
                if (deltaMs in 0.5f..250f) {
                    frameCount++
                    accumulatedFrameMs += deltaMs
                }

                val elapsedNs = frameTimeNanos - windowStartNs
                if (elapsedNs >= 500_000_000L && frameCount > 0) {
                    val elapsedSec = elapsedNs / 1_000_000_000f
                    val panelHz = readDisplayRefreshRate().roundToInt().coerceAtLeast(30)
                    val measuredFps = (frameCount / elapsedSec).roundToInt().coerceIn(1, panelHz)
                    val avgMs = accumulatedFrameMs / frameCount
                    updateOverlayMetrics(measuredFps, avgMs, panelHz)

                    windowStartNs = frameTimeNanos
                    frameCount = 0
                    accumulatedFrameMs = 0f
                }
            }

            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        profileRepo = GameProfileRepository(applicationContext)
        backgroundAppRepo = BackgroundAppRepository(applicationContext)
        shizukuManager = ShizukuManager(applicationContext)
        tweaksController = DeviceTweaksController(
            context = applicationContext,
            backgroundAppRepository = backgroundAppRepo,
            shizukuManager = shizukuManager,
        )

        _runtimeStatus.update {
            it.copy(isServiceConnected = true)
        }

        serviceScope.launch {
            combine(
                profileRepo.fpsOverlayEnabledFlow,
                profileRepo.overlayBoostModeFlow,
            ) { enabled, mode -> enabled to mode }
                .collect { (enabled, mode) ->
                    currentBoostMode = mode
                    _runtimeStatus.update { it.copy(activeBoostMode = mode) }
                    if (enabled) {
                        showOverlayIfNeeded()
                        updateModeVisuals(mode)
                    } else {
                        hideOverlay()
                    }
                }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg == "com.android.systemui" || pkg == "android") return
            val isGame = pkg in knownGamePackages
            _runtimeStatus.update {
                it.copy(
                    foregroundPackage = pkg,
                    isKnownGameForeground = isGame,
                )
            }
            updateGameDotIndicator(isGame)
        }
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    override fun onDestroy() {
        hideOverlay()
        _runtimeStatus.update {
            OverlayRuntimeStatus(isServiceConnected = false)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlayIfNeeded() {
        if (rootContainer != null) return

        val density = resources.displayMetrics.density
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        val padH = (10f * density).roundToInt()
        val padV = (5f * density).roundToInt()
        val dotSize = (7f * density).roundToInt()
        val dotMargin = (6f * density).roundToInt()

        val pillBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f * density
            setColor(Color.argb(224, 5, 7, 11))
            setStroke((1f * density).roundToInt().coerceAtLeast(1), Color.argb(200, 0, 168, 255))
        }

        val dotDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(
                if (_runtimeStatus.value.isKnownGameForeground) {
                    Color.rgb(41, 231, 255)
                } else {
                    Color.rgb(0, 230, 118)
                },
            )
        }

        val dot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                marginEnd = dotMargin
            }
            background = dotDrawable
        }

        val fpsText = TextView(this).apply {
            text = "-- FPS"
            setTextColor(Color.rgb(41, 231, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
        }

        val metaText = TextView(this).apply {
            text = "  •  VSYNC"
            setTextColor(Color.rgb(190, 212, 240))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            typeface = Typeface.MONOSPACE
        }

        val modeBadge = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            val badgePadH = (6f * density).roundToInt()
            val badgePadV = (2f * density).roundToInt()
            setPadding(badgePadH, badgePadV, badgePadH, badgePadV)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = (8f * density).roundToInt()
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(dot)
            addView(fpsText)
            addView(metaText)
            addView(modeBadge)
        }

        // Expandable inline Game Boost Mode selector (Power Save, Balanced, Advance)
        val drawer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = if (isDrawerExpanded) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = (6f * density).roundToInt()
            }
        }

        val btnMap = mutableMapOf<OverlayBoostMode, TextView>()
        OverlayBoostMode.entries.forEachIndexed { index, mode ->
            val btn = TextView(this).apply {
                text = mode.displayName
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = Typeface.MONOSPACE
                setTypeface(typeface, Typeface.BOLD)
                val bPadH = (8f * density).roundToInt()
                val bPadV = (4f * density).roundToInt()
                setPadding(bPadH, bPadV, bPadH, bPadV)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    if (index > 0) marginStart = (5f * density).roundToInt()
                }
                setOnClickListener {
                    selectBoostModeFromOverlay(mode)
                }
            }
            btnMap[mode] = btn
            drawer.addView(btn)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = pillBackground
            setPadding(padH, padV, padH, padV)
            addView(topRow)
            addView(drawer)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16f * density).roundToInt()
            y = (48f * density).roundToInt()
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var hasDragged = false

        topRow.setOnTouchListener { _, motionEvent ->
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = motionEvent.rawX
                    touchY = motionEvent.rawY
                    hasDragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = motionEvent.rawX - touchX
                    val dy = motionEvent.rawY - touchY
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        hasDragged = true
                        params.x = startX + dx.roundToInt()
                        params.y = startY + dy.roundToInt()
                        runCatching { windowManager.updateViewLayout(container, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasDragged) {
                        isDrawerExpanded = !isDrawerExpanded
                        drawer.visibility = if (isDrawerExpanded) View.VISIBLE else View.GONE
                        runCatching { windowManager.updateViewLayout(container, params) }
                    }
                    true
                }
                else -> false
            }
        }

        runCatching {
            windowManager.addView(container, params)
            rootContainer = container
            statusDotView = dot
            fpsTextView = fpsText
            metaTextView = metaText
            modeBadgeView = modeBadge
            modeDrawerView = drawer
            modeButtons = btnMap
            layoutParams = params
            updateModeVisuals(currentBoostMode)
            startChoreographer()
            _runtimeStatus.update { it.copy(isOverlayVisible = true) }
        }
    }

    private fun selectBoostModeFromOverlay(mode: OverlayBoostMode) {
        currentBoostMode = mode
        updateModeVisuals(mode)
        _runtimeStatus.update { it.copy(activeBoostMode = mode) }

        serviceScope.launch {
            profileRepo.setOverlayBoostMode(mode)
            val snapshot = profileRepo.snapshotFlow.first()
            if (snapshot.isActive) {
                val profiles = profileRepo.profilesFlow.first()
                val activeProfile = profiles.firstOrNull { it.id == snapshot.activeProfileId } ?: profiles.first()
                val trimPkgs = withContext(Dispatchers.IO) {
                    backgroundAppRepo.discoverLaunchableApps()
                        .filter { !it.isProtected && it.isSelected }
                        .map { it.packageName }
                }
                val outcome = withContext(Dispatchers.IO) {
                    tweaksController.applyBoostProfile(
                        profile = activeProfile,
                        boostMode = mode,
                        selectedPackagesToTrim = trimPkgs,
                        previousSnapshot = snapshot,
                    )
                }
                profileRepo.saveSnapshot(outcome.snapshot)
            }
        }
    }

    private fun updateModeVisuals(mode: OverlayBoostMode) {
        val density = resources.displayMetrics.density
        val accent = modeAccentColor(mode)

        modeBadgeView?.apply {
            text = "${mode.shortBadge} ${if (isDrawerExpanded) "▲" else "▾"}"
            setTextColor(accent)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6f * density
                setColor(Color.argb(46, Color.red(accent), Color.green(accent), Color.blue(accent)))
                setStroke((1f * density).roundToInt().coerceAtLeast(1), accent)
            }
        }

        modeButtons.forEach { (btnMode, view) ->
            val selected = btnMode == mode
            val btnAccent = modeAccentColor(btnMode)
            view.setTextColor(if (selected) Color.rgb(5, 7, 11) else Color.rgb(176, 194, 222))
            view.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 7f * density
                if (selected) {
                    setColor(btnAccent)
                } else {
                    setColor(Color.rgb(17, 25, 38))
                    setStroke((1f * density).roundToInt().coerceAtLeast(1), Color.rgb(30, 46, 71))
                }
            }
        }

        rootContainer?.let { container ->
            layoutParams?.let { params ->
                runCatching { windowManager.updateViewLayout(container, params) }
            }
        }
    }

    private fun modeAccentColor(mode: OverlayBoostMode): Int {
        return when (mode) {
            OverlayBoostMode.POWER_SAVE -> Color.rgb(0, 230, 118)
            OverlayBoostMode.BALANCED -> Color.rgb(0, 168, 255)
            OverlayBoostMode.ADVANCE -> Color.rgb(41, 231, 255)
        }
    }

    private fun hideOverlay() {
        stopChoreographer()
        rootContainer?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        rootContainer = null
        statusDotView = null
        fpsTextView = null
        metaTextView = null
        modeBadgeView = null
        modeDrawerView = null
        modeButtons = emptyMap()
        layoutParams = null
        _runtimeStatus.update {
            it.copy(
                isOverlayVisible = false,
                measuredFps = 0,
                frameTimeMs = 0f,
            )
        }
    }

    private fun startChoreographer() {
        if (choreographerRunning) return
        choreographerRunning = true
        windowStartNs = 0L
        lastFrameNs = 0L
        frameCount = 0
        accumulatedFrameMs = 0f
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopChoreographer() {
        if (!choreographerRunning) return
        choreographerRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    private fun updateOverlayMetrics(fps: Int, avgMs: Float, panelHz: Int) {
        val tempC = readBatteryTempC()
        val tempText = tempC?.let { String.format(Locale.US, "%.1f°C", it) } ?: "${panelHz}Hz"
        fpsTextView?.text = "$fps FPS"
        metaTextView?.text = String.format(Locale.US, "  %.1fms  %s", avgMs, tempText)
        modeBadgeView?.text = "${currentBoostMode.shortBadge} ${if (isDrawerExpanded) "▲" else "▾"}"
        _runtimeStatus.update {
            it.copy(
                measuredFps = fps,
                frameTimeMs = avgMs,
            )
        }
    }

    private fun updateGameDotIndicator(isKnownGame: Boolean) {
        val drawable = statusDotView?.background as? GradientDrawable ?: return
        drawable.setColor(
            if (isKnownGame) Color.rgb(41, 231, 255) else Color.rgb(0, 230, 118),
        )
    }

    @Suppress("DEPRECATION")
    private fun readDisplayRefreshRate(): Float {
        return runCatching {
            val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display: Display = dm.getDisplay(Display.DEFAULT_DISPLAY) ?: return 60f
            display.mode.refreshRate
        }.getOrDefault(60f)
    }

    private fun readBatteryTempC(): Float? {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val raw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        return if (raw != null && raw != Int.MIN_VALUE && raw > 0) raw / 10f else null
    }

    companion object {
        private val _runtimeStatus = MutableStateFlow(OverlayRuntimeStatus())
        val runtimeStatus: StateFlow<OverlayRuntimeStatus> = _runtimeStatus.asStateFlow()

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            if (_runtimeStatus.value.isServiceConnected) return true

            val expectedComponent = ComponentName(context, FpsOverlayAccessibilityService::class.java)
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledList = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            val viaManager = enabledList.any { info ->
                val svcInfo = info.resolveInfo?.serviceInfo ?: return@any false
                svcInfo.packageName == expectedComponent.packageName &&
                    svcInfo.name == expectedComponent.className
            }
            if (viaManager) return true

            val enabledSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false

            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledSetting)
            while (splitter.hasNext()) {
                val componentName = ComponentName.unflattenFromString(splitter.next())
                if (componentName == expectedComponent) {
                    return true
                }
            }
            return false
        }
    }
}
