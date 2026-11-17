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
    private fun pipParams(autoEnter: Boolean): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        // Допустимые пропорции окна PiP: от 1:2.39 до 2.39:1
        var w = vm.pipWidth.coerceAtLeast(1)
        var h = vm.pipHeight.coerceAtLeast(1)
        val ratio = w.toFloat() / h
        if (ratio > 2.39f) { w = 239; h = 100 } else if (ratio < 0.42f) { w = 42; h = 100 }
        val b = PictureInPictureParams.Builder().setAspectRatio(Rational(w, h))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: плавный автоматический вход в PiP при выходе жестом или кнопкой «Домой»
            b.setAutoEnterEnabled(autoEnter)
            b.setSeamlessResizeEnabled(true)
        }
        return b.build()
    }

    /** Вызывается из плеера: обновляет пропорции окна PiP и разрешает автоматический вход. */
    fun updatePip(autoEnter: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            pipParams(autoEnter)?.let { setPictureInPictureParams(it) }
        } catch (_: Exception) {
        }
    }

    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                pipParams(autoEnter = true)?.let { enterPictureInPictureMode(it) }
            } catch (_: Exception) {
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // До Android 12 входим вручную; с 12-й это делает setAutoEnterEnabled
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && vm.pipEligible) enterPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        vm.inPip = isInPictureInPictureMode
    }
}
