package dev.ronova.prism.ui.liquid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import dev.ronova.prism.data.GlassMode
import dev.ronova.prism.ui.glass.LocalGlassMode
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Кнопка «жидкого стекла» из каталога библиотеки Backdrop (LiquidButton), адаптированная под Prism:
 * можно задать форму и размер, отключить кнопку, повесить долгое нажатие и свою подложку.
 *
 * Механика та же, что в образце: линза + размытие + vibrancy, при касании кнопка «наливается»
 * и тянется за пальцем (InteractiveHighlight).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = Capsule(),
    isInteractive: Boolean = true,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    height: Dp = 48.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    shadow: Shadow? = Shadow(radius = 12.dp, color = Color.Black.copy(alpha = 0.10f)),
    blurRadius: Dp = 2.dp,
    lensHeight: Dp = 12.dp,
    lensAmount: Dp = 24.dp,
    onLongClick: (() -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val pressable = isInteractive && enabled

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    // Без изображения позади (например, поверх видео) эффекты ничего не показали бы — не тратим на них слой.
    val hasBackdrop = backdrop !== com.kyant.backdrop.backdrops.emptyBackdrop() &&
            LocalGlassMode.current == GlassMode.FULL

    Row(
        modifier
            .drawBackdrop(
                backdrop = if (hasBackdrop) backdrop else com.kyant.backdrop.backdrops.emptyBackdrop(),
                shape = { shape },
                effects = if (hasBackdrop) {
                    {
                        vibrancy()
                        blur(blurRadius.toPx())
                        // линза умеет только скруглённые прямоугольники (RoundedCornerShape, CircleShape)
                        if (this.shape is CornerBasedShape) {
                            lens(lensHeight.toPx(), lensAmount.toPx())
                        }
                    }
                } else {
                    {}
                },
                shadow = if (shadow != null) ({ shadow }) else null,
                layerBlock = if (pressable) {
                    {
                        val width = size.width
                        val height = size.height

                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 4f.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                    maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                    maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    onDrawSurface?.invoke(this)
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .combinedClickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick
            )
            .then(
                if (pressable) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .height(height)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
