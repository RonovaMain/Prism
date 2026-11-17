package dev.ronova.prism.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ronova.prism.ui.theme.GlassTokens
import dev.ronova.prism.ui.theme.LocalGlass
import dev.ronova.prism.ui.theme.PrismAmber
import dev.ronova.prism.ui.theme.PrismCyan
import dev.ronova.prism.ui.theme.PrismPink
import dev.ronova.prism.ui.theme.PrismViolet

private fun DrawScope.orb(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawOrbs(g: GlassTokens, a: Float, b: Float) {
    val w = size.width
    val h = size.height
    val k = if (g.dark) 1f else 0.8f
    orb(Offset(w * (0.12f + 0.55f * a), h * (0.10f + 0.12f * b)), w * 0.95f, PrismViolet, 0.36f * k)
    orb(Offset(w * (0.95f - 0.45f * b), h * (0.42f + 0.18f * a)), w * 0.85f, PrismCyan, 0.24f * k)
    orb(Offset(w * (0.15f + 0.35f * b), h * (0.97f - 0.25f * a)), w * 0.95f, PrismPink, 0.28f * k)
}

/** Фон приложения: тёмная база и медленно дрейфующие цветные пятна, сквозь которые «светится» стекло. */
@Composable
fun PrismBackground(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val g = LocalGlass.current
    if (animate) {
        val t = rememberInfiniteTransition(label = "bg")
        val a by t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse),
            label = "a",
        )
        val b by t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(34000, easing = LinearEasing), RepeatMode.Reverse),
            label = "b",
        )
        Box(
            modifier
                .background(g.background)
                .drawBehind { drawOrbs(g, a, b) },
            content = content,
        )
    } else {
        Box(
            modifier
                .background(g.background)
                .drawBehind { drawOrbs(g, 0.3f, 0.4f) },
            content = content,
        )
    }
}

/** Треугольник со скруглёнными углами (форма «Play» и призмы). */
fun roundedTrianglePath(p1: Offset, p2: Offset, p3: Offset, r: Float): Path {
    val pts = listOf(p1, p2, p3)
    val path = Path()
    for (i in 0 until 3) {
        val p = pts[i]
        val prev = pts[(i + 2) % 3]
        val next = pts[(i + 1) % 3]
        val d1 = prev - p
        val d2 = next - p
        val l1 = d1.getDistance()
        val l2 = d2.getDistance()
        val rr = minOf(r, l1 / 2f, l2 / 2f)
        val a = p + d1 * (rr / l1)
        val b = p + d2 * (rr / l2)
        if (i == 0) path.moveTo(a.x, a.y) else path.lineTo(a.x, a.y)
        path.quadraticBezierTo(p.x, p.y, b.x, b.y)
    }
    path.close()
    return path
}

/** Логотип Prism: стеклянная призма в форме Play с переливом. */
@Composable
fun PrismLogo(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // рамка треугольника: x от 0.20 до 0.80 — ровно по центру холста
        val a = Offset(w * 0.20f, h * 0.17f)
        val b = Offset(w * 0.20f, h * 0.83f)
        val c = Offset(w * 0.80f, h * 0.50f)
        val m = Offset(w * 0.20f, h * 0.50f)
        val tri = roundedTrianglePath(a, b, c, w * 0.10f)
        clipPath(tri) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(PrismCyan, PrismViolet, PrismPink, PrismAmber),
                    start = Offset(w * 0.20f, h * 0.20f),
                    end = Offset(w * 0.82f, h * 0.62f),
                )
            )
            // верхняя грань — светлая
            val top = Path().apply {
                moveTo(a.x, a.y)
                lineTo(c.x, c.y)
                lineTo(m.x, m.y)
                close()
            }
            drawPath(
                top,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.05f)),
                    start = a,
                    end = Offset(w * 0.6f, h * 0.5f),
                ),
            )
            // нижняя грань — темнее
            val bottom = Path().apply {
                moveTo(m.x, m.y)
                lineTo(c.x, c.y)
                lineTo(b.x, b.y)
                close()
            }
            drawPath(bottom, Color.Black.copy(alpha = 0.18f))
        }
        drawPath(tri, Color.White.copy(alpha = 0.75f), style = Stroke(width = w * 0.018f))
    }
}

/** Папка в фирменном стиле: стеклянная, с переливом призмы. */
@Composable
fun PrismFolderIcon(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = w * 0.14f
        val back = Brush.linearGradient(
            colors = listOf(PrismCyan, PrismViolet, PrismPink),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        )
        drawRoundRect(back, Offset(w * 0.08f, h * 0.16f), Size(w * 0.42f, h * 0.30f), CornerRadius(r, r))
        drawRoundRect(back, Offset(w * 0.08f, h * 0.28f), Size(w * 0.84f, h * 0.58f), CornerRadius(r, r))
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.12f)),
                start = Offset(0f, h * 0.4f),
                end = Offset(w, h * 0.9f),
            ),
            topLeft = Offset(w * 0.08f, h * 0.42f),
            size = Size(w * 0.84f, h * 0.44f),
            cornerRadius = CornerRadius(r, r),
        )
    }
}
