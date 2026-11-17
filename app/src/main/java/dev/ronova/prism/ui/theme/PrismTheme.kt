package dev.ronova.prism.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.ronova.prism.data.ThemeMode

val PrismCyan = Color(0xFF5CE1FF)
val PrismViolet = Color(0xFF8B7BFF)
val PrismPink = Color(0xFFFF6FD0)
val PrismAmber = Color(0xFFFFC857)

/** Фирменный перелив призмы. */
fun prismBrush(): Brush = Brush.linearGradient(listOf(PrismCyan, PrismViolet, PrismPink, PrismAmber))

@Immutable
class GlassTokens(
    val dark: Boolean,
    val background: Color,
    val text: Color,
    val textDim: Color,
    val fillTop: Color,
    val fillBottom: Color,
    val rimBright: Color,
    val rimDim: Color,
    val highlight: Color,
    val shadow: Color,
    val scrim: Color,
    val accent: Color,
    /** Подложка для меню и панелей, чтобы текст читался поверх любого фона. */
    val menuBase: Color,
)

val DarkGlass = GlassTokens(
    dark = true,
    background = Color(0xFF07070D),
    text = Color.White,
    textDim = Color.White.copy(alpha = 0.66f),
    fillTop = Color.White.copy(alpha = 0.17f),
    fillBottom = Color.White.copy(alpha = 0.06f),
    rimBright = Color.White.copy(alpha = 0.85f),
    rimDim = Color.White.copy(alpha = 0.10f),
    highlight = Color.White.copy(alpha = 0.20f),
    shadow = Color.Black.copy(alpha = 0.55f),
    scrim = Color.Black.copy(alpha = 0.55f),
    accent = Color(0xFFA99BFF),
    menuBase = Color(0xFF14121F).copy(alpha = 0.72f),
)

val LightGlass = GlassTokens(
    dark = false,
    background = Color(0xFFEEEBF8),
    text = Color(0xFF15121F),
    textDim = Color(0xFF15121F).copy(alpha = 0.60f),
    fillTop = Color.White.copy(alpha = 0.78f),
    fillBottom = Color.White.copy(alpha = 0.46f),
    rimBright = Color.White,
    rimDim = Color.White.copy(alpha = 0.50f),
    highlight = Color.White.copy(alpha = 0.90f),
    shadow = Color(0xFF3B2F7A).copy(alpha = 0.28f),
    scrim = Color(0xFF15121F).copy(alpha = 0.35f),
    accent = Color(0xFF5B49E8),
    menuBase = Color(0xFFFFFFFF).copy(alpha = 0.55f),
)

val LocalGlass = staticCompositionLocalOf { DarkGlass }

private val PrismTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

@Composable
fun resolveDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun PrismTheme(dark: Boolean, content: @Composable () -> Unit) {
    val tokens = if (dark) DarkGlass else LightGlass
    val scheme = if (dark) {
        darkColorScheme(
            primary = tokens.accent,
            onPrimary = Color.Black,
            background = tokens.background,
            onBackground = tokens.text,
            surface = tokens.background,
            onSurface = tokens.text,
            onSurfaceVariant = tokens.textDim,
        )
    } else {
        lightColorScheme(
            primary = tokens.accent,
            onPrimary = Color.White,
            background = tokens.background,
            onBackground = tokens.text,
            surface = tokens.background,
            onSurface = tokens.text,
            onSurfaceVariant = tokens.textDim,
        )
    }
    CompositionLocalProvider(LocalGlass provides tokens) {
        MaterialTheme(colorScheme = scheme, typography = PrismTypography) {
            CompositionLocalProvider(LocalContentColor provides tokens.text) {
                content()
            }
        }
    }
}
