package dev.ronova.prism.ui.liquid

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Капсула (полностью скруглённые края). Это обычная форма Compose, поэтому линзовый эффект
 * библиотеки понимает её без дополнительных зависимостей.
 */
fun Capsule(): CornerBasedShape = RoundedCornerShape(percent = 50)
