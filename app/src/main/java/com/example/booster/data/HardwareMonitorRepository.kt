package com.example.booster.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.view.Display
import com.example.booster.model.CpuCoreClock
import com.example.booster.model.HardwareTelemetry
import com.example.booster.model.ThermalStateLevel
import com.example.booster.model.ThermalZoneReading
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.roundToInt

class HardwareMonitorRepository(private val context: Context) {

    private var prevCpuTotal: Long = 0L
    private var prevCpuIdle: Long = 0L

    private val ramHistory = ArrayDeque<Float>(24)
    private val cpuHistory = ArrayDeque<Float>(24)

    fun sample(latestUiFrameTimeMs: Float): HardwareTelemetry {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availRamMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(0L)
        val ramPct = ((usedRamMb.toFloat() / totalRamMb.toFloat()) * 100f).coerceIn(0f, 100f)
        val cachedMb = readMemInfoFieldMb("Cached:")

        val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val clocks = readCpuCoreClocks(cpuCores)
        val (cpuLoadPct, cpuSource) = readCpuLoadPercent(clocks)

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val batteryTempC = if (rawTemp != null && rawTemp != Int.MIN_VALUE && rawTemp > 0) {
            rawTemp / 10f
        } else {
            null
        }
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt() else 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val systemThermalStatus = readSystemThermalStatus(powerManager)
        val thermalZones = readReadableThermalZones()
        val maxObservedTemp = listOfNotNull(
            batteryTempC,
            thermalZones.maxOfOrNull { it.tempCelsius },
        ).maxOrNull()

        val thermalLevel = classifyThermalLevel(
            tempC = maxObservedTemp,
            systemThermalStatus = systemThermalStatus,
        )

        val (freeStorageGb, totalStorageGb) = readDataStorageGb()
        val (refreshRateHz, supportedRates) = readDisplayRefreshRates()
        val networkLabel = readNetworkLabel()

        val socLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN } ?: Build.HARDWARE
        } else {
            Build.HARDWARE
        }
        val abiLabel = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

        pushHistory(ramHistory, ramPct)
        pushHistory(cpuHistory, cpuLoadPct ?: estimateClockRatioPercent(clocks))

        return HardwareTelemetry(
            totalRamMb = totalRamMb,
            usedRamMb = usedRamMb,
            availableRamMb = availRamMb,
            ramUsagePercent = ramPct,
            cachedRamMb = cachedMb,
            isLowMemory = memInfo.lowMemory,
            cpuCores = cpuCores,
            cpuLoadPercent = cpuLoadPct,
            cpuLoadSource = cpuSource,
            cpuClocks = clocks,
            socLabel = socLabel,
            abiLabel = abiLabel,
            batteryTempC = batteryTempC,
            batteryLevelPct = batteryPct,
            isCharging = isCharging,
            batteryVoltageMv = voltageMv,
            systemThermalStatus = systemThermalStatus,
            thermalLevel = thermalLevel,
            thermalZones = thermalZones,
            isPowerSaveMode = powerManager.isPowerSaveMode,
            storageFreeGb = freeStorageGb,
            storageTotalGb = totalStorageGb,
            refreshRateHz = refreshRateHz,
            supportedRefreshRatesHz = supportedRates,
            uiFrameTimeMs = latestUiFrameTimeMs,
            networkLabel = networkLabel,
            ramHistory = ramHistory.toList(),
            cpuHistory = cpuHistory.toList(),
        )
    }

    private fun pushHistory(queue: ArrayDeque<Float>, value: Float) {
        if (queue.size >= 24) {
            queue.removeFirst()
        }
        queue.addLast(value.coerceIn(0f, 100f))
    }

    private fun readMemInfoFieldMb(prefix: String): Long? {
        return runCatching {
            File("/proc/meminfo").useLines { lines ->
                lines.firstOrNull { it.startsWith(prefix) }
                    ?.split(Regex("\\s+"))
                    ?.getOrNull(1)
                    ?.toLongOrNull()
                    ?.div(1024L)
            }
        }.getOrNull()
    }

    private fun readCpuCoreClocks(cores: Int): List<CpuCoreClock> {
        return (0 until cores.coerceAtMost(12)).map { index ->
            val curKhz = readIntFromFile("/sys/devices/system/cpu/cpu$index/cpufreq/scaling_cur_freq")
            val maxKhz = readIntFromFile("/sys/devices/system/cpu/cpu$index/cpufreq/cpuinfo_max_freq")
                ?: readIntFromFile("/sys/devices/system/cpu/cpu$index/cpufreq/scaling_max_freq")
            CpuCoreClock(
                coreIndex = index,
                currentMhz = curKhz?.div(1000),
                maxMhz = maxKhz?.div(1000),
            )
        }
    }

    private fun readCpuLoadPercent(clocks: List<CpuCoreClock>): Pair<Float?, String> {
        val procStatLoad = runCatching {
            RandomAccessFile("/proc/stat", "r").use { reader ->
                val line = reader.readLine() ?: return@use null
                if (!line.startsWith("cpu ")) return@use null
                val parts = line.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
                if (parts.size < 4) return@use null
                val idle = parts[3] + (parts.getOrNull(4) ?: 0L)
                val total = parts.sum()
                val diffTotal = total - prevCpuTotal
                val diffIdle = idle - prevCpuIdle
                prevCpuTotal = total
                prevCpuIdle = idle
                if (diffTotal <= 0L || prevCpuTotal == total && diffTotal == total) {
                    null
                } else {
                    (((diffTotal - diffIdle).toFloat() / diffTotal.toFloat()) * 100f).coerceIn(0f, 100f)
                }
            }
        }.getOrNull()

        if (procStatLoad != null) {
            return procStatLoad to "/proc/stat"
        }

        val freqUtil = estimateClockRatioPercentOrNull(clocks)
        if (freqUtil != null) {
            return freqUtil to "CPU Freq Scaling"
        }

        return null to "Restricted by SELinux"
    }

    private fun estimateClockRatioPercentOrNull(clocks: List<CpuCoreClock>): Float? {
        val pairs = clocks.mapNotNull { clock ->
            val cur = clock.currentMhz
            val max = clock.maxMhz
            if (cur != null && max != null && max > 0) {
                (cur.toFloat() / max.toFloat()) * 100f
            } else {
                null
            }
        }
        return if (pairs.isNotEmpty()) pairs.average().toFloat().coerceIn(0f, 100f) else null
    }

    private fun estimateClockRatioPercent(clocks: List<CpuCoreClock>): Float {
        return estimateClockRatioPercentOrNull(clocks) ?: 0f
    }

    private fun readSystemThermalStatus(powerManager: PowerManager): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "API <29 (Battery Temp)"
        return when (powerManager.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "Nominal"
            PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth"
            PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Load"
            PowerManager.THERMAL_STATUS_SEVERE -> "Severe Throttling"
            PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Heat"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown Risk"
            else -> "Nominal"
        }
    }

    private fun readReadableThermalZones(): List<ThermalZoneReading> {
        val dir = File("/sys/class/thermal")
        val zones = dir.listFiles { file -> file.name.startsWith("thermal_zone") } ?: return emptyList()
        return zones.sortedBy { it.name }.take(8).mapNotNull { zone ->
            val rawTemp = readIntFromFile(File(zone, "temp").absolutePath) ?: return@mapNotNull null
            val tempC = if (rawTemp > 1000) rawTemp / 1000f else rawTemp.toFloat()
            if (tempC !in 10f..110f) return@mapNotNull null
            val type = runCatching { File(zone, "type").readText().trim() }.getOrNull()
                ?.takeIf { it.isNotBlank() } ?: zone.name
            ThermalZoneReading(name = type, tempCelsius = tempC)
        }
    }

    private fun classifyThermalLevel(tempC: Float?, systemThermalStatus: String): ThermalStateLevel {
        if (systemThermalStatus.contains("Severe", true) ||
            systemThermalStatus.contains("Critical", true) ||
            systemThermalStatus.contains("Emergency", true)
        ) {
            return ThermalStateLevel.HOT
        }
        if (tempC == null) return ThermalStateLevel.UNKNOWN
        return when {
            tempC >= 43f -> ThermalStateLevel.HOT
            tempC >= 37.5f -> ThermalStateLevel.WARM
            else -> ThermalStateLevel.COOL
        }
    }

    private fun readDataStorageGb(): Pair<Float, Float> {
        return runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes.toFloat() / (1024f * 1024f * 1024f)
            val avail = stat.availableBytes.toFloat() / (1024f * 1024f * 1024f)
            avail to total
        }.getOrDefault(0f to 0f)
    }

    @Suppress("DEPRECATION")
    private fun readDisplayRefreshRates(): Pair<Float, List<Float>> {
        return runCatching {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display: Display = dm.getDisplay(Display.DEFAULT_DISPLAY) ?: return 60f to listOf(60f)
            val activeRate = display.mode.refreshRate
            val supported = display.supportedModes
                .map { it.refreshRate.roundToInt().toFloat() }
                .distinct()
                .sorted()
            activeRate to supported
        }.getOrDefault(60f to listOf(60f))
    }

    private fun readNetworkLabel(): String {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return "Offline"
            val caps = cm.getNetworkCapabilities(network) ?: return "Offline"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Active"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Data"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Connected"
            }
        }.getOrDefault("Unavailable")
    }

    private fun readIntFromFile(path: String): Int? {
        return runCatching {
            File(path).readText().trim().toIntOrNull()
        }.getOrNull()
    }
}
