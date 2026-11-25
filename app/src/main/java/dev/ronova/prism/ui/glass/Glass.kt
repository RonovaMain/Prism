package dev.ronova.prism.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import dev.ronova.prism.ui.liquid.Capsule
import dev.ronova.prism.ui.liquid.LiquidBottomTab
import dev.ronova.prism.ui.liquid.LiquidBottomTabs
import dev.ronova.prism.ui.liquid.LiquidButton
import dev.ronova.prism.ui.liquid.LiquidSlider
import dev.ronova.prism.ui.liquid.LiquidToggle
import dev.ronova.prism.ui.theme.GlassTokens
import dev.ronova.prism.ui.theme.LocalGlass

/**
 * Весь интерфейс Prism собран на библиотеке «жидкого стекла» Backdrop (Kyant0/AndroidLiquidGlass):
 * размытие, линза, vibrancy, блик по кромке и тень рисует она. Этот файл — тонкая прослойка с теми же
 * именами, что были раньше (GlassButton, GlassSurface, GlassChip…), поэтому экраны почти не менялись.
 */

/** Рисовать ли тени под стеклом. Приложение и плеер выставляют своё значение из настроек. */
val LocalGlassShadows = staticCompositionLocalOf { true }

/**
 * Что находится ПОЗАДИ стекла: стекло преломляет и размывает именно это.
 * PrismBackground кладёт сюда запись фона; в плеере (видео — отдельная SurfaceView, в слой Compose оно
 * не попадает) остаётся пустая подложка, и стекло рисуется только заливкой, бликом и тенью.
 */
val LocalBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }

internal fun Color.scaleAlpha(f: Float): Color = copy(alpha = (alpha * f).coerceIn(0f, 1f))

// Параметры стекла. Меняйте здесь, если нужно сильнее/слабее.
private val SurfaceBlur = 6.dp
private val SurfaceLensHeight = 10.dp
private val SurfaceLensAmount = 20.dp

/** Полупрозрачная заливка поверх размытого фона: на светлой теме слабее, чтобы линза оставалась видна. */
private fun glassFill(g: GlassTokens, strength: Float, base: Color, tint: Color): DrawScope.() -> Unit = {
    if (base.alpha > 0f) drawRect(base)
    val k = if (g.dark) 1f else 0.55f
    drawRect(
        Brush.verticalGradient(listOf(g.fillTop.scaleAlpha(strength * k), g.fillBottom.scaleAlpha(strength * k)))
    )
    if (tint.alpha > 0f) drawRect(tint)
}

private fun glassShadowOrNull(g: GlassTokens, enabled: Boolean, elevation: Dp): Shadow? =
    if (enabled && elevation.value > 0f) Shadow(radius = elevation * 1.2f, color = g.shadow) else null

/** Нажатие с «пружинным» эффектом: элемент слегка проседает и упруго возвращается (для карточек и строк). */
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
 * Основа дизайна — стеклянная панель библиотеки: drawBackdrop с размытием, линзой, бликом и тенью.
 * [refraction] = false отключает линзу (для больших карточек в списках — заметно легче для GPU).
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = Color.Transparent,
    strength: Float = 1f,
    elevation: Dp = 10.dp,
    base: Color = Color.Transparent,
    refraction: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val g = LocalGlass.current
    val backdrop = LocalBackdrop.current
    val shadowsOn = LocalGlassShadows.current
    val hasBackdrop = backdrop !== emptyBackdrop()
    val fill = remember(g, strength, base, tint) { glassFill(g, strength, base, tint) }
    val highlight = remember(strength) { Highlight.Default.copy(alpha = strength.coerceIn(0f, 1f)) }
    val shadow = glassShadowOrNull(g, shadowsOn, elevation)

    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = if (hasBackdrop) {
                {
                    vibrancy()
                    blur(SurfaceBlur.toPx())
                    if (refraction && this.shape is CornerBasedShape) {
                        lens(SurfaceLensHeight.toPx(), SurfaceLensAmount.toPx())
                    }
                }
            } else {
                {}
            },
            highlight = { highlight },
            shadow = if (shadow != null) ({ shadow }) else null,
            onDrawSurface = fill,
        ),
        content = content,
    )
}

/** Круглая кнопка с иконкой — LiquidButton из библиотеки. */
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
    val shadowsOn = LocalGlassShadows.current
    val fill = remember(g, strength) { glassFill(g, strength, Color.Transparent, Color.Transparent) }
    LiquidButton(
        onClick = onClick,
        backdrop = LocalBackdrop.current,
        modifier = modifier.size(size),
        shape = CircleShape,
        enabled = enabled,
        surfaceColor = if (active) g.accent.copy(alpha = 0.38f) else Color.Unspecified,
        height = size,
        contentPadding = PaddingValues(0.dp),
        shadow = glassShadowOrNull(g, shadowsOn, 6.dp),
        lensHeight = (size / 4).coerceAtLeast(8.dp),
        lensAmount = (size / 2).coerceAtLeast(16.dp),
        onDrawSurface = fill,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) g.text else g.textDim,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Крупная круглая кнопка с произвольным содержимым (play/pause в плеере) — тоже LiquidButton. */
@Composable
fun GlassRoundButton(
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color = Color.Transparent,
    strength: Float = 1f,
    elevation: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val g = LocalGlass.current
    val shadowsOn = LocalGlassShadows.current
    val fill = remember(g, strength) { glassFill(g, strength, Color.Transparent, Color.Transparent) }
    LiquidButton(
        onClick = onClick,
        backdrop = LocalBackdrop.current,
        modifier = modifier.size(size),
        shape = CircleShape,
        surfaceColor = if (tint.alpha > 0f) tint else Color.Unspecified,
        height = size,
        contentPadding = PaddingValues(0.dp),
        shadow = glassShadowOrNull(g, shadowsOn, elevation),
        lensHeight = (size / 4).coerceAtLeast(8.dp),
        lensAmount = (size / 2).coerceAtLeast(16.dp),
        onDrawSurface = fill,
    ) {
        Box(Modifier.size(size), content = content)
    }
}

/** Капсула-чип (фильтры, действия) — тоже LiquidButton. */
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
    val shadowsOn = LocalGlassShadows.current
    val fill = remember(g) { glassFill(g, 1f, Color.Transparent, Color.Transparent) }
    LiquidButton(
        onClick = onClick,
        backdrop = LocalBackdrop.current,
        modifier = modifier,
        shape = Capsule(),
        surfaceColor = if (selected) g.accent.copy(alpha = 0.42f) else Color.Unspecified,
        height = if (dense) 32.dp else 40.dp,
        contentPadding = PaddingValues(horizontal = if (dense) 12.dp else 16.dp),
        shadow = glassShadowOrNull(g, shadowsOn, 3.dp),
        lensHeight = 8.dp,
        lensAmount = 16.dp,
        onDrawSurface = fill,
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

/** Маленькая «капсула» поверх изображений: длительность, качество. */
@Composable
fun GlassTag(text: String, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier = modifier,
        shape = Capsule(),
        tint = Color.Black.copy(alpha = 0.32f),
        elevation = 0.dp,
        refraction = false,
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

/**
 * Плавающая панель вкладок — LiquidBottomTabs из библиотеки: бегунок можно тянуть пальцем,
 * при касании он «наливается» и преломляет иконки.
 * [backdrop] по умолчанию — фон приложения; HomeScreen передаёт фон вместе с прокручиваемым содержимым.
 */
@Composable
fun GlassTabBar(
    items: List<TabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalBackdrop.current,
) {
    val g = LocalGlass.current
    // LiquidBottomTabs запоминает лямбды при первой композиции, поэтому отдаём ему устойчивые обёртки
    val selectedState = rememberUpdatedState(selected)
    val onSelectState = rememberUpdatedState(onSelect)
    val selectedIndex = remember { { selectedState.value } }
    val selectIndex = remember<(Int) -> Unit> { { i -> onSelectState.value(i) } }

    LiquidBottomTabs(
        selectedTabIndex = selectedIndex,
        onTabSelected = selectIndex,
        backdrop = backdrop,
        tabsCount = items.size,
        modifier = modifier,
    ) {
        items.forEachIndexed { i, item ->
            LiquidBottomTab(onClick = { onSelect(i) }) {
                Icon(item.icon, contentDescription = null, tint = g.text, modifier = Modifier.size(24.dp))
                Text(
                    item.label,
                    color = g.text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Переключатель — LiquidToggle из библиотеки (бегунок тянется и преломляет трек). */
@Composable
fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    val checkedState = rememberUpdatedState(checked)
    val changeState = rememberUpdatedState(onCheckedChange)
    val selected = remember { { checkedState.value } }
    val onSelect = remember<(Boolean) -> Unit> { { value -> changeState.value(value) } }
    LiquidToggle(
        selected = selected,
        onSelect = onSelect,
        backdrop = LocalBackdrop.current,
        modifier = modifier,
        accentColor = g.accent,
        trackColor = if (g.dark) Color.White.copy(alpha = 0.20f) else Color(0xFF15121F).copy(alpha = 0.18f),
    )
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
    GlassSurface(modifier, shape = RoundedCornerShape(26.dp), elevation = 4.dp, refraction = false) {
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

/**
 * Выпадающее стеклянное меню (открывается рядом с кнопкой, внутри её родительского Box).
 * Popup — отдельное окно, записи фона из основного окна ему недоступны, поэтому внутри меню стекло
 * рисуется заливкой, бликом и тенью без преломления; читаемость обеспечивает menuBase.
 */
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
        CompositionLocalProvider(LocalBackdrop provides emptyBackdrop()) {
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

/** Ползунок перемотки — LiquidSlider из библиотеки: бегунок-«капля» растёт при касании и преломляет трек. */
@Composable
fun GlassSeekBar(
    fraction: Float,
    enabled: Boolean,
    onChange: (Float) -> Unit,
    onChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val g = LocalGlass.current
    val fractionState = rememberUpdatedState(fraction.coerceIn(0f, 1f))
    val value = remember { { fractionState.value } }
    val changeState = rememberUpdatedState(onChange)
    val finishedState = rememberUpdatedState(onChangeFinished)
    LiquidSlider(
        value = value,
        onValueChange = { changeState.value(it) },
        valueRange = 0f..1f,
        visibilityThreshold = 0.001f,
        backdrop = LocalBackdrop.current,
        modifier = modifier.heightIn(min = 40.dp),
        enabled = enabled,
        onValueChangeFinished = { finishedState.value(it) },
        accentColor = g.accent,
        trackColor = if (g.dark) Color.White.copy(alpha = 0.22f) else Color(0xFF15121F).copy(alpha = 0.16f),
    )
}
