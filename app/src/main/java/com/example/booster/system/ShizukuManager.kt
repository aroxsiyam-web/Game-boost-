package com.example.booster.system

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import com.example.BuildConfig
import com.example.IShizukuBoostService
import com.example.booster.model.ShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class ShizukuManager(private val context: Context) {

    private val _status = MutableStateFlow(ShizukuStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private var userService: IShizukuBoostService? = null
    private var listenersRegistered = false

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(context, ShizukuBoostUserService::class.java))
            .daemon(false)
            .processNameSuffix("boost_service")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service == null || !service.isBinderAlive) {
                userService = null
                refreshStatus("Shizuku service binder unavailable")
                return
            }
            userService = IShizukuBoostService.Stub.asInterface(service)
            val shellId = runCatching { userService?.shellInfo }.getOrNull()
            _status.update {
                it.copy(
                    isServiceBound = true,
                    shellIdentity = shellId,
                    lastMessage = "Shizuku UserService connected",
                )
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
            _status.update {
                it.copy(
                    isServiceBound = false,
                    shellIdentity = null,
                    lastMessage = "Shizuku UserService disconnected",
                )
            }
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshStatus("Shizuku binder active")
        if (hasPermission()) {
            bindServiceIfAuthorized()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        userService = null
        refreshStatus("Shizuku binder stopped")
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                refreshStatus(
                    if (granted) "Shizuku authorization granted" else "Shizuku authorization denied by user",
                )
                if (granted) {
                    bindServiceIfAuthorized()
                }
            }
        }

    fun register() {
        if (!listenersRegistered) {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            listenersRegistered = true
        }
        refreshStatus()
        if (hasPermission()) {
            bindServiceIfAuthorized()
        }
    }

    fun unregister() {
        if (listenersRegistered) {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            listenersRegistered = false
        }
        runCatching {
            if (userService != null) {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
                userService = null
            }
        }
    }

    fun refreshStatus(messageOverride: String? = null) {
        val installed = isShizukuInstalled()
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val granted = if (binderAlive) hasPermission() else false
        val uid = if (binderAlive && granted) runCatching { Shizuku.getUid() }.getOrNull() else null
        val defaultMsg = when {
            !installed -> "Shizuku app not installed (optional)"
            !binderAlive -> "Shizuku installed, waiting for service start"
            !granted -> "Shizuku active — tap Authorize to grant permission"
            userService != null -> "Authorized & UserService bound"
            else -> "Authorized — binding UserService..."
        }
        _status.update { current ->
            current.copy(
                isInstalled = installed,
                isBinderAlive = binderAlive,
                isPermissionGranted = granted,
                isServiceBound = userService != null && binderAlive,
                privilegeUid = uid,
                lastMessage = messageOverride ?: defaultMsg,
            )
        }
    }

    fun requestAuthorization() {
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!binderAlive) {
            refreshStatus("Start Shizuku service first before authorizing")
            return
        }
        if (Shizuku.isPreV11()) {
            refreshStatus("Shizuku v11+ required")
            return
        }
        if (hasPermission()) {
            bindServiceIfAuthorized()
            refreshStatus("Shizuku already authorized")
            return
        }
        Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
    }

    private fun bindServiceIfAuthorized() {
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!binderAlive || !hasPermission() || userService != null) return
        runCatching {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        }.onFailure { err ->
            refreshStatus("Bind failed: ${err.message ?: "unknown"}")
        }
    }

    private fun hasPermission(): Boolean {
        return runCatching {
            !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    private fun isShizukuInstalled(): Boolean {
        return runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo("moe.shizuku.privileged.api", PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo("moe.shizuku.privileged.api", 0)
            }
            true
        }.getOrDefault(false)
    }

    suspend fun getAnimationScales(): Triple<String?, String?, String?> = withContext(Dispatchers.IO) {
        val svc = userService ?: return@withContext Triple(null, null, null)
        runCatching {
            Triple(
                svc.getGlobalSetting("window_animation_scale").ifBlank { null },
                svc.getGlobalSetting("transition_animation_scale").ifBlank { null },
                svc.getGlobalSetting("animator_duration_scale").ifBlank { null },
            )
        }.getOrDefault(Triple(null, null, null))
    }

    suspend fun setAnimationScales(window: String, transition: String, animator: String): Boolean =
        withContext(Dispatchers.IO) {
            val svc = userService ?: return@withContext false
            runCatching {
                val ok1 = svc.putGlobalSetting("window_animation_scale", window)
                val ok2 = svc.putGlobalSetting("transition_animation_scale", transition)
                val ok3 = svc.putGlobalSetting("animator_duration_scale", animator)
                ok1 && ok2 && ok3
            }.getOrDefault(false)
        }

    suspend fun setGameMode(packageName: String, mode: String): String = withContext(Dispatchers.IO) {
        val svc = userService ?: return@withContext "Shizuku UserService not bound"
        runCatching { svc.setGameMode(packageName, mode) }
            .getOrElse { "Shizuku RPC error: ${it.message}" }
    }

    suspend fun trimBackgroundApp(packageName: String): String = withContext(Dispatchers.IO) {
        val svc = userService ?: return@withContext "Shizuku UserService not bound"
        runCatching { svc.trimBackgroundApp(packageName) }
            .getOrElse { "Shizuku RPC error: ${it.message}" }
    }

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 4201
    }
}
