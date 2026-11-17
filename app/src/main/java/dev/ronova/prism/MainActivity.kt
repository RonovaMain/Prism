package dev.ronova.prism

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronova.prism.ui.PrismRoot
import dev.ronova.prism.ui.PrismViewModel
import dev.ronova.prism.ui.theme.resolveDark

class MainActivity : ComponentActivity() {

    private val vm: PrismViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent, coldStart = true)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val dark = resolveDark(settings.themeMode)
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                this@MainActivity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            PrismRoot(vm = vm, dark = dark, onFinish = { finishAndRemoveTask() })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent, coldStart = false)
    }

    /** «Открыть с помощью Prism» из любого файлового менеджера, браузера или чата. */
    private fun handleIntent(intent: Intent?, coldStart: Boolean) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            vm.finishOnExit = coldStart
            vm.playUri(uri, external = coldStart)
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshPermission()
    }

    // ---- картинка в картинке
    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                enterPictureInPictureMode(
                    PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
                )
            } catch (_: Exception) {
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (vm.pipEligible) enterPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        vm.inPip = isInPictureInPictureMode
    }
}
