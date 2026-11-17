package dev.ronova.prism.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import dev.ronova.prism.ui.screens.HomeScreen
import dev.ronova.prism.ui.screens.PlayerScreen
import dev.ronova.prism.ui.theme.PrismTheme

@Composable
fun PrismRoot(vm: PrismViewModel, dark: Boolean, onFinish: () -> Unit) {
    PrismTheme(dark) {
        Box(Modifier.fillMaxSize()) {
            // Библиотека остаётся под плеером (чтобы не терять положение списка), но фон при этом не анимируется
            HomeScreen(vm, animate = vm.playRequest == null)
            vm.playRequest?.let { request ->
                key(request) {
                    PlayerScreen(vm, request) {
                        vm.closePlayer()
                        if (vm.finishOnExit) onFinish()
                    }
                }
            }
        }
    }
}
