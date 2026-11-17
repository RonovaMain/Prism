package dev.ronova.prism.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.ronova.prism.ui.theme.LocalGlass
import dev.ronova.prism.ui.theme.PrismCyan
import dev.ronova.prism.ui.theme.PrismPink
import dev.ronova.prism.ui.theme.PrismViolet

internal fun Color.scaleAlpha(f: Float): Color = copy(alpha = (alpha * f).coerceIn(0f, 1f))


private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

/**
 * Мягкая тень, которая рисуется ТОЛЬКО снаружи формы.
 *
 * Системный Modifier.shadow под полупрозрачным стеклом просвечивает сквозь заливку: внутри
 * кнопок и карточек появлялись ромбы, квадраты и прямоугольные «призраки». Здесь тень строится
 * из слоёв обводки с убывающей прозрачностью, а внутренняя область вырезается, поэтому стекло
 * остаётся чистым.
 */
fun Modifier.glassShadow(shape: Shape, elevation: Dp, color: Color): Modifier {
    if (elevation.value <= 0f || color.alpha <= 0f) return this
    return drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = outline.toPath()
        val reach = elevation.toPx() * 1.5f
        val dy = elevation.toPx() * 0.35f
        val layers = (reach / 2.5.dp.toPx()).toInt().coerceIn(5, 12)
        val perLayer = color.alpha * 1.25f / layers
        clipPath(path, ClipOp.Difference) {
            translate(top = dy) {
                for (i in layers downTo 1) {
                    val w = 2f * reach * i / layers
                    val falloff = 1f - (i - 1f) / layers
                    drawOutline(
                        outline = outline,
                        color = color.copy(alpha = (perLayer * (0.55f + 0.45f * falloff)).coerceIn(0f, 1f)),
                        style = Stroke(width = w),
                    )
                }
            }
        }
    }
}

/** Нажатие с «пружинным» эффектом, как у жидкого стекла: элемент слегка проседает и упруго возвращается. */
fun Modifier.glassClick(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f),
        label = "press",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

/**
 * Основа дизайна — «жидкое стекло»: полупрозрачная заливка, мягкий блик в верхнем углу,
 * светящаяся кромка и лёгкая цветная тень.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = Color.Transparent,
    strength: Float = 1f,
    elevation: Dp = 10.dp,
    base: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit,
) {
    val g = LocalGlass.current
    val rim = remember(g) {
        Brush.linearGradient(
            colorStops = arrayOf(
                0f to g.rimBright,
                0.38f to g.rimDim,
                0.70f to g.rimDim.scaleAlpha(0.5f),
                1f to g.rimBright.scaleAlpha(0.45f),
            ),
            start = Offset.Zero,
            end = Offset.Infinite,
        )
    }
    Box(
        modifier
            .glassShadow(shape, elevation, g.shadow)
            .clip(shape)
            .background(base)
            .background(Brush.verticalGradient(listOf(g.fillTop.scaleAlpha(strength), g.fillBottom.scaleAlpha(strength))))
            .background(tint)
            .drawBehind {
                // мягкий блик в верхнем левом углу
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(g.highlight.scaleAlpha(strength), Color.Transparent),
                        center = Offset(size.width * 0.18f, 0f),
                        radius = size.maxDimension * 0.75f,
                    )
                )
                // световая полоса вдоль верхней кромки
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = if (g.dark) 0.09f else 0.35f), Color.Transparent),
                        endY = size.height * 0.38f,
                    )
                )
            }
            .border(1.dp, rim, shape),
        content = content,
    )
}

@Composable
fun GlassButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    active: Boolean = false,
    enabled: Boolean = true,
    strength: Float = 1f,
    contentDescription: String? = null,
) {
    val g = LocalGlass.current
    GlassSurface(
        modifier = modifier.size(size).glassClick(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        tint = if (active) g.accent.copy(alpha = 0.38f) else Color.Transparent,
        strength = strength,
        elevation = 6.dp,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) g.text else g.textDim,
            modifier = Modifier.align(Alignment.Center).size(iconSize),
        )
    }
}

@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    dense: Boolean = false,
) {
    val g = LocalGlass.current
    GlassSurface(
        modifier = modifier.glassClick(onClick = onClick),
        shape = RoundedCornerShape(50),
        tint = if (selected) g.accent.copy(alpha = 0.42f) else Color.Transparent,
        elevation = 3.dp,
    ) {
        Row(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = if (dense) 12.dp else 16.dp, vertical = if (dense) 6.dp else 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = g.text, modifier = Modifier.size(16.dp))
            Text(
                text,
                color = g.text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

/** Маленькая «капсула» поверх изображений: длительность, качество. */
@Composable
fun GlassTag(text: String, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        tint = Color.Black.copy(alpha = 0.32f),
        elevation = 0.dp,
    ) {
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

data class TabItem(val icon: ImageVector, val label: String)

/** Плавающая панель вкладок: индикатор упруго «перетекает» между пунктами. */
@Composable
fun GlassTabBar(
    items: List<TabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val g = LocalGlass.current
    GlassSurface(
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(35.dp),
        strength = 1.15f,
        elevation = 16.dp,
        base = g.menuBase.scaleAlpha(0.55f),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
            val tabWidth = maxWidth / items.size
            val x by animateDpAsState(
                targetValue = tabWidth * selected,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
                label = "tab",
            )
            Box(
                Modifier
                    .offset(x = x)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(29.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                PrismCyan.copy(alpha = 0.30f),
                                PrismViolet.copy(alpha = 0.38f),
                                PrismPink.copy(alpha = 0.30f),
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = if (g.dark) 0.35f else 0.8f), RoundedCornerShape(29.dp))
            )
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { i, item ->
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .glassClick(onClick = { onSelect(i) }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = if (i == selected) g.text else g.textDim,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            item.label,
                            color = if (i == selected) g.text else g.textDim,
                            fontSize = 11.sp,
                            fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    val on by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 500f), label = "on")
    val thumbX by animateDpAsState(
        if (checked) 22.dp else 0.dp,
        spring(dampingRatio = 0.5f, stiffness = 520f),
        label = "thumb",
    )
    val trackShape = RoundedCornerShape(50)
    Box(
        modifier
            .width(54.dp)
            .height(32.dp)
            .glassClick { onCheckedChange(!checked) }
            .clip(trackShape)
            .background(if (g.dark) Color.White.copy(alpha = 0.14f) else Color(0xFF15121F).copy(alpha = 0.12f))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        PrismCyan.copy(alpha = 0.75f * on),
                        PrismViolet.copy(alpha = 0.85f * on),
                        PrismPink.copy(alpha = 0.75f * on),
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = if (g.dark) 0.30f else 0.7f), trackShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .offset(x = thumbX)
                .size(26.dp)
                .glassShadow(CircleShape, 4.dp, g.shadow)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE6E1FF))))
                .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape)
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val g = LocalGlass.current
    GlassSurface(modifier, shape = RoundedCornerShape(26.dp), elevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Icon(leading, contentDescription = null, tint = g.textDim, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, color = g.textDim, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = g.text),
                    cursorBrush = SolidColor(g.accent),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailing != null) trailing()
        }
    }
}

/** Выпадающее стеклянное меню (открывается рядом с кнопкой, внутри её родительского Box). */
@Composable
fun GlassDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val g = LocalGlass.current
    val yOffset = with(LocalDensity.current) { 54.dp.roundToPx() }
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, yOffset),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        GlassSurface(
            modifier = modifier.width(240.dp),
            shape = RoundedCornerShape(26.dp),
            strength = 1.3f,
            elevation = 20.dp,
            base = g.menuBase,
        ) {
            Column(Modifier.padding(8.dp), content = content)
        }
    }
}

@Composable
fun GlassMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
) {
    val g = LocalGlass.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) g.accent.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = g.text, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text,
            color = g.text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = g.text, modifier = Modifier.size(18.dp))
    }
}

/** Стеклянный ползунок перемотки с перетекающим цветным треком и упругим бегунком. */
@Composable
fun GlassSeekBar(
    fraction: Float,
    enabled: Boolean,
    onChange: (Float) -> Unit,
    onChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val g = LocalGlass.current
    var widthPx by remember { mutableIntStateOf(1) }
    var dragging by remember { mutableStateOf(false) }
    var current by remember { mutableFloatStateOf(0f) }
    val thumbScale by animateFloatAsState(
        targetValue = if (dragging) 1.45f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "seekThumb",
    )
    val f = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .height(40.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { off ->
                    val v = (off.x / widthPx).coerceIn(0f, 1f)
                    onChange(v)
                    onChangeFinished(v)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { off ->
                        dragging = true
                        current = (off.x / widthPx).coerceIn(0f, 1f)
                        onChange(current)
                    },
                    onDragEnd = {
                        dragging = false
                        onChangeFinished(current)
                    },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        current = (change.position.x / widthPx).coerceIn(0f, 1f)
                        onChange(current)
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val trackH = 6.dp.toPx()
            val cy = size.height / 2f
            val r = trackH / 2f
            drawRoundRect(
                color = if (g.dark) Color.White.copy(alpha = 0.22f) else Color(0xFF15121F).copy(alpha = 0.16f),
                topLeft = Offset(0f, cy - r),
                size = Size(size.width, trackH),
                cornerRadius = CornerRadius(r, r),
            )
            val px = size.width * f
            if (px > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(PrismCyan, PrismViolet, PrismPink)),
                    topLeft = Offset(0f, cy - r),
                    size = Size(px, trackH),
                    cornerRadius = CornerRadius(r, r),
                )
            }
            val tr = 9.dp.toPx() * thumbScale
            drawCircle(Color.Black.copy(alpha = 0.28f), radius = tr + 2.dp.toPx(), center = Offset(px, cy + 1.dp.toPx()))
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, Color(0xFFE4DEFF)),
                    center = Offset(px - tr * 0.3f, cy - tr * 0.3f),
                    radius = tr * 1.5f,
                ),
                radius = tr,
                center = Offset(px, cy),
            )
            drawCircle(Color.White.copy(alpha = 0.9f), radius = tr, center = Offset(px, cy), style = Stroke(1.dp.toPx()))
        }
    }
}
