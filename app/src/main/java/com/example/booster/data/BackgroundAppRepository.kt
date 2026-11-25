package com.example.booster.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.booster.model.BackgroundAppItem

data class TrimExecutionResult(
    val trimmedCount: Int,
    val freedRamMb: Long,
)

class BackgroundAppRepository(private val context: Context) {

    private val protectedPackages = setOf(
        context.packageName,
        "moe.shizuku.privileged.api",
        "com.termux",
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.vending",
        "com.android.phone",
        "com.android.providers.telephony",
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "com.tencent.ig",
        "com.pubg.imobile",
        "com.rekoo.pubgm",
        "com.vng.pubgmobile",
        "com.mojang.minecraftpe",
    )

    fun discoverLaunchableApps(previousSelections: Map<String, Boolean> = emptyMap()): List<BackgroundAppItem> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val defaultHomePkg = resolveDefaultHomePackage(pm)

        return resolveInfos
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .map { appInfo ->
                val pkg = appInfo.packageName
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val protectionReason = when {
                    pkg == context.packageName -> "Active Booster"
                    pkg == defaultHomePkg -> "Home Launcher"
                    pkg in setOf("moe.shizuku.privileged.api", "com.termux") -> "System Tool"
                    pkg.startsWith("com.dts.freefire") ||
                        pkg == "com.tencent.ig" ||
                        pkg == "com.pubg.imobile" ||
                        pkg == "com.mojang.minecraftpe" -> "Game Profile"
                    pkg in protectedPackages -> "Core Service"
                    isSystemApp && (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0 -> "System App"
                    else -> null
                }
                val isProtected = protectionReason != null
                val defaultSelected = !isProtected
                BackgroundAppItem(
                    packageName = pkg,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    isProtected = isProtected,
                    protectionReason = protectionReason,
                    isSelected = if (isProtected) false else (previousSelections[pkg] ?: defaultSelected),
                )
            }
            .sortedWith(compareBy<BackgroundAppItem> { it.isProtected }.thenBy { it.appName.lowercase() })
    }

    fun trimBackgroundProcessesSafely(packages: List<String>): TrimExecutionResult {
        if (packages.isEmpty()) return TrimExecutionResult(0, 0L)
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val beforeInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(beforeInfo)

        var count = 0
        for (pkg in packages) {
            if (pkg in protectedPackages || pkg == context.packageName) continue
            runCatching {
                am.killBackgroundProcesses(pkg)
                count++
            }
        }

        val afterInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(afterInfo)
        val freedBytes = (afterInfo.availMem - beforeInfo.availMem).coerceAtLeast(0L)
        val freedMb = freedBytes / (1024L * 1024L)
        return TrimExecutionResult(trimmedCount = count, freedRamMb = freedMb)
    }

    private fun resolveDefaultHomePackage(pm: PackageManager): String? {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(homeIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return info?.activityInfo?.packageName
    }
}
