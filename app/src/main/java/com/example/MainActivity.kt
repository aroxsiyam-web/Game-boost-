package com.example

import android.os.Bundle
import android.view.Choreographer
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.booster.ui.BoosterAppShell
import com.example.booster.ui.BoosterViewModel
import com.example.ui.theme.IvcGameBoosterTheme

class MainActivity : ComponentActivity() {

    private lateinit var boosterViewModel: BoosterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        setContent {
            val viewModel: BoosterViewModel = viewModel(
                factory = BoosterViewModel.factory(application),
            )
            boosterViewModel = viewModel
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Reversible Keep-Screen-On flag synchronized with active boost snapshot
            val keepScreenOn = uiState.snapshot.isActive && uiState.snapshot.keepScreenOnChanged
            LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // Real Choreographer frame timing for our own booster dashboard UI (never fakes game FPS)
            DisposableEffect(Unit) {
                var lastFrameNs = 0L
                val callback = object : Choreographer.FrameCallback {
                    override fun doFrame(frameTimeNanos: Long) {
                        if (lastFrameNs != 0L) {
                            val diffMs = (frameTimeNanos - lastFrameNs) / 1_000_000f
                            viewModel.reportDashboardFrameTimeMs(diffMs)
                        }
                        lastFrameNs = frameTimeNanos
                        Choreographer.getInstance().postFrameCallback(this)
                    }
                }
                Choreographer.getInstance().postFrameCallback(callback)
                onDispose {
                    Choreographer.getInstance().removeFrameCallback(callback)
                }
            }

            IvcGameBoosterTheme {
                BoosterAppShell(
                    state = uiState,
                    viewModel = viewModel,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::boosterViewModel.isInitialized) {
            boosterViewModel.refreshSystemIntegrations()
            boosterViewModel.refreshBackgroundApps()
        }
    }
}
