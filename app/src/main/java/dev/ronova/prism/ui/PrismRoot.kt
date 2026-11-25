package dev.ronova.prism.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronova.prism.ui.glass.LocalGlassMode
import dev.ronova.prism.ui.glass.LocalGlassShadows
import dev.ronova.prism.ui.screens.HomeScreen
import dev.ronova.prism.ui.screens.PlayerScreen
import dev.ronova.prism.ui.theme.PrismTheme

@Composable
fun PrismRoot(vm: PrismViewModel, dark: Boolean, onFinish: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    PrismTheme(dark) {
        CompositionLocalProvider(LocalGlassMode provides settings.glassMode) {
        Box(Modifier.fillMaxSize()) {
            // Библиотека остаётся под плеером (чтобы не терять положение списка), но фон при этом не анимируется
            CompositionLocalProvider(LocalGlassShadows provides settings.uiShadows) {
                HomeScreen(vm, animate = vm.playRequest == null)
            }
            vm.playRequest?.let { request ->
                key(request) {
                    // в плеере тени отдельная настройка (по умолчанию выключены — максимум прозрачности)
                    CompositionLocalProvider(LocalGlassShadows provides settings.playerShadows) {
                        PlayerScreen(vm, request) {
                            vm.closePlayer()
                            if (vm.finishOnExit) onFinish()
                        }
                    }
                }
            }
        }
        }
    }
}
