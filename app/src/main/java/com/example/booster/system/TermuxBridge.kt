package com.example.booster.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.booster.model.TermuxCommandTemplate
import com.example.booster.model.TermuxStatus

class TermuxBridge(private val context: Context) {

    fun checkStatus(lastDispatchedCommand: String? = null): TermuxStatus {
        val installed = runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(TERMUX_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(TERMUX_PACKAGE, 0)
            }
            true
        }.getOrDefault(false)

        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            TERMUX_PERMISSION,
        ) == PackageManager.PERMISSION_GRANTED

        return TermuxStatus(
            isInstalled = installed,
            hasRunCommandPermission = permissionGranted,
            lastDispatchedCommand = lastDispatchedCommand,
        )
    }

    fun builtInTemplates(): List<TermuxCommandTemplate> = listOf(
        TermuxCommandTemplate(
            id = "mem_thermal",
            title = "Memory & Load Snapshot",
            description = "Inspects real free memory, swap pressure, and kernel load averages in a Termux window.",
            executablePath = "/data/data/com.termux/files/usr/bin/bash",
            arguments = listOf("-c", "free -h && echo '--- Load Avg ---' && cat /proc/loadavg && read -p 'Press Enter to close...'"),
            workingDirectory = "/data/data/com.termux/files/home",
            runInBackground = false,
            safetyNote = "Read-only diagnostic script. Does not alter system files or thermal policies.",
        ),
        TermuxCommandTemplate(
            id = "net_ping",
            title = "Game Network Latency Check",
            description = "Measures real round-trip ping and packet jitter before starting a competitive match.",
            executablePath = "/data/data/com.termux/files/usr/bin/bash",
            arguments = listOf("-c", "ping -c 4 1.1.1.1 && read -p 'Press Enter to close...'"),
            workingDirectory = "/data/data/com.termux/files/home",
            runInBackground = false,
            safetyNote = "Sends 4 ICMP echo probes to 1.1.1.1. Read-only network check.",
        ),
        TermuxCommandTemplate(
            id = "top_procs",
            title = "Top CPU Consumers",
            description = "Lists top active processes visible to the Termux user environment.",
            executablePath = "/data/data/com.termux/files/usr/bin/bash",
            arguments = listOf("-c", "top -b -n 1 | head -n 18 && read -p 'Press Enter to close...'"),
            workingDirectory = "/data/data/com.termux/files/home",
            runInBackground = false,
            safetyNote = "Read-only process snapshot.",
        ),
        TermuxCommandTemplate(
            id = "storage_df",
            title = "Storage & Partition Health",
            description = "Checks /data partition utilization so texture/chunk caches have healthy free space.",
            executablePath = "/data/data/com.termux/files/usr/bin/bash",
            arguments = listOf("-c", "df -h /data && read -p 'Press Enter to close...'"),
            workingDirectory = "/data/data/com.termux/files/home",
            runInBackground = false,
            safetyNote = "Read-only storage check.",
        ),
    )

    fun createCustomTemplate(shellCommand: String, runInBackground: Boolean): TermuxCommandTemplate {
        val cleaned = shellCommand.trim()
        val wrapped = if (runInBackground) {
            cleaned
        } else {
            "$cleaned; echo ''; read -p 'Press Enter to close...'"
        }
        return TermuxCommandTemplate(
            id = "custom_cmd",
            title = "Custom Termux Command",
            description = "User-authored Termux shell command requiring explicit review before dispatch.",
            executablePath = "/data/data/com.termux/files/usr/bin/bash",
            arguments = listOf("-c", wrapped),
            workingDirectory = "/data/data/com.termux/files/home",
            runInBackground = runInBackground,
            safetyNote = "Verify the command carefully. IVC SIYAM never runs Termux commands automatically.",
        )
    }

    /**
     * Dispatches the Termux RUN_COMMAND intent ONLY after explicit user confirmation in the UI.
     */
    fun dispatchConfirmedCommand(template: TermuxCommandTemplate): Result<String> {
        val status = checkStatus()
        if (!status.isInstalled) {
            return Result.failure(IllegalStateException("Termux (com.termux) is not installed on this device."))
        }
        if (!status.hasRunCommandPermission) {
            return Result.failure(
                IllegalStateException("Missing com.termux.permission.RUN_COMMAND. Grant it in App Info -> Permissions."),
            )
        }

        val intent = Intent().apply {
            setClassName(TERMUX_PACKAGE, TERMUX_SERVICE)
            action = ACTION_RUN_COMMAND
            putExtra(EXTRA_PATH, template.executablePath)
            putExtra(EXTRA_ARGUMENTS, template.arguments.toTypedArray())
            putExtra(EXTRA_WORKDIR, template.workingDirectory)
            putExtra(EXTRA_BACKGROUND, template.runInBackground)
        }

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            "Sent to Termux: ${template.title}"
        }
    }

    companion object {
        const val TERMUX_PACKAGE = "com.termux"
        const val TERMUX_SERVICE = "com.termux.app.RunCommandService"
        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
        const val TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND"

        private const val EXTRA_PATH = "com.termux.RUN_COMMAND_PATH"
        private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    }
}
