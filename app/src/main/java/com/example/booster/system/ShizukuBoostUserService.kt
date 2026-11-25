package com.example.booster.system

import android.os.Build
import com.example.IShizukuBoostService
import kotlin.system.exitProcess

/**
 * Privileged UserService executed inside the Shizuku server process (ADB Shell UID 2000 or Root UID 0).
 * Every exposed method strictly validates inputs against allowlists and never disables thermal protections.
 */
class ShizukuBoostUserService : IShizukuBoostService.Stub() {

    private val allowedGlobalSettings = setOf(
        "window_animation_scale",
        "transition_animation_scale",
        "animator_duration_scale",
    )

    private val allowedGameModes = setOf("standard", "performance", "battery")
    private val packageRegex = Regex("^[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+$")
    private val numericRegex = Regex("^\\d+(\\.\\d+)?$")

    override fun getGlobalSetting(key: String?): String {
        if (key == null || key !in allowedGlobalSettings) return ""
        return runCommand(arrayOf("settings", "get", "global", key)).trim()
    }

    override fun putGlobalSetting(key: String?, value: String?): Boolean {
        if (key == null || key !in allowedGlobalSettings) return false
        val sanitized = value?.trim() ?: return false
        val isValid = sanitized == "null" || (numericRegex.matches(sanitized) && (sanitized.toFloatOrNull() ?: -1f) in 0.0f..2.0f)
        if (!isValid) return false
        runCommand(arrayOf("settings", "put", "global", key, sanitized))
        return true
    }

    override fun setGameMode(packageName: String?, mode: String?): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return "Android 12+ required for cmd game mode"
        }
        if (packageName == null || !packageRegex.matches(packageName)) {
            return "Invalid package name"
        }
        if (mode == null || mode !in allowedGameModes) {
            return "Unsupported game mode"
        }
        return runCommand(arrayOf("cmd", "game", "mode", mode, packageName)).trim()
            .ifBlank { "Applied mode '$mode' for $packageName" }
    }

    override fun trimBackgroundApp(packageName: String?): String {
        if (packageName == null || !packageRegex.matches(packageName)) {
            return "Invalid package"
        }
        if (packageName.contains("thermal", ignoreCase = true) ||
            packageName == "com.android.systemui" ||
            packageName == "com.android.settings"
        ) {
            return "Protected package skipped"
        }
        return runCommand(arrayOf("am", "kill", packageName)).trim()
            .ifBlank { "Trimmed cached background processes for $packageName" }
    }

    override fun getShellInfo(): String {
        return runCommand(arrayOf("id")).trim()
    }

    override fun destroy() {
        exitProcess(0)
    }

    private fun runCommand(args: Array<String>): String {
        return runCatching {
            val process = ProcessBuilder(*args)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        }.getOrElse { error ->
            "Error: ${error.message ?: "Command failed"}"
        }
    }
}
