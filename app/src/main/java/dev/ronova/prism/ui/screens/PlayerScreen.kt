package dev.ronova.prism.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronova.prism.MainActivity
import dev.ronova.prism.R
import dev.ronova.prism.data.Aspect
import dev.ronova.prism.data.PlayRequest
import dev.ronova.prism.player.PrismPlayer
import dev.ronova.prism.ui.PrismViewModel
import dev.ronova.prism.ui.glass.GlassButton
import dev.ronova.prism.ui.glass.GlassChip
import dev.ronova.prism.ui.glass.GlassMenuItem
import dev.ronova.prism.ui.glass.GlassSeekBar
import dev.ronova.prism.ui.glass.GlassSurface
import dev.ronova.prism.ui.glass.glassClick
import dev.ronova.prism.ui.glass.scaleAlpha
import dev.ronova.prism.ui.theme.DarkGlass
import dev.ronova.prism.ui.theme.LocalGlass
import dev.ronova.prism.ui.theme.PrismCyan
import dev.ronova.prism.ui.theme.PrismPink
import dev.ronova.prism.ui.theme.PrismViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class Panel { TRACKS, SPEED, ASPECT }

private class Hud(val icon: ImageVector, val text: String, val progress: Float? = null)

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private fun timeText(ms: Long): String {
    val t = (ms / 1000).coerceAtLeast(0)
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun Activity.applyBrightness(v: Float) {
    val lp = window.attributes
    lp.screenBrightness = v
    window.attributes = lp
}

private val SPEED_VALUES = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f, 4f)

private fun speedText(s: Float): String = if (s == s.toInt().toFloat()) "${s.toInt()}×" else "$s×"

@Composable
fun PlayerScreen(vm: PrismViewModel, request: PlayRequest, onExit: () -> Unit) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val player = remember {
        try {
            PrismPlayer(ctx, settings.hwMode, settings.subtitleEncoding)
        } catch (e: Throwable) {
            null
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (player == null) {
            BackHandler { onExit() }
            ErrorPane(onRetry = null, onExit = onExit)
        } else {
            PlayerContent(vm, request, player, onExit)
        }
    }
}

@Composable
private fun BoxScope.ErrorPane(onRetry: (() -> Unit)?, onExit: () -> Unit) {
    val g = DarkGlass
    GlassSurface(
        Modifier.align(Alignment.Center).padding(24.dp).widthIn(max = 420.dp),
        shape = RoundedCornerShape(32.dp),
        elevation = 12.dp,
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.player_error),
                color = g.text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onRetry != null) {
                    GlassChip(stringResource(R.string.retry), true, onRetry)
                }
                GlassChip(stringResource(R.string.back), false, onExit)
            }
        }
    }
}

@Composable
private fun PlayerContent(vm: PrismViewModel, request: PlayRequest, player: PrismPlayer, onExit: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(ctx) { ctx.findActivity() }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val resumeMap by vm.resume.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val g = DarkGlass // поверх видео всегда тёмное стекло

    var index by remember { mutableIntStateOf(request.index.coerceIn(0, request.items.lastIndex)) }
    var retry by remember { mutableIntStateOf(0) }
    val source = request.items[index]
    val sourceState = rememberUpdatedState(source)
    val settingsState = rememberUpdatedState(settings)

    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    var speed by remember { mutableFloatStateOf(1f) }
    var aspect by remember { mutableStateOf(settings.defaultAspect) }
    var position by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var hud by remember { mutableStateOf<Hud?>(null) }
    var hudTick by remember { mutableIntStateOf(0) }
    var boost by remember { mutableStateOf(false) }
    var landscape by remember { mutableStateOf(settings.autoLandscape) }

    val audioManager = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume) }
    var brightness by remember {
        mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f)
    }

    fun showHud(h: Hud) {
        hud = h
        hudTick++
    }

    fun switchTo(newIndex: Int) {
        if (settingsState.value.rememberPosition) {
            vm.saveResume(sourceState.value.key, player.timeMs(), player.lengthMs)
        }
        index = newIndex
    }

    // ---- открытие файла
    LaunchedEffect(index, retry) {
        val start = if (settings.rememberPosition) (resumeMap[source.key] ?: 0L) else 0L
        position = 0L
        player.open(source, start, settings.networkCaching)
    }
    // скорость и формат кадра применяем, когда видео реально пошло
    LaunchedEffect(index, player.isPlaying) {
        if (player.isPlaying) {
            player.setRate(if (boost) 2f else speed)
            player.setAspect(aspect)
        }
    }
    LaunchedEffect(aspect) { player.setAspect(aspect) }

    // ---- опрос позиции
    LaunchedEffect(Unit) {
        while (true) {
            if (!dragging) position = player.timeMs()
            delay(250)
        }
    }

    // ---- запоминание места просмотра
    LaunchedEffect(index) {
        while (true) {
            delay(5000)
            if (settingsState.value.rememberPosition && player.isPlaying) {
                vm.saveResume(sourceState.value.key, player.timeMs(), player.lengthMs)
            }
        }
    }

    // ---- конец ролика
    LaunchedEffect(player.endedTick) {
        if (player.endedTick > 0) {
            vm.saveResume(source.key, player.lengthMs, player.lengthMs)
            if (settings.autoPlayNext && index < request.items.lastIndex) {
                index += 1
            } else {
                controlsVisible = true
            }
        }
    }

    // ---- автоскрытие панели
    LaunchedEffect(controlsVisible, player.isPlaying, interaction, panel) {
        if (controlsVisible && player.isPlaying && panel == null) {
            delay(3500)
            controlsVisible = false
        }
    }
    LaunchedEffect(hudTick) {
        if (hud != null) {
            delay(900)
            hud = null
        }
    }

    // ---- экран не гаснет во время просмотра; можно уйти в «картинка в картинке»
    SideEffect {
        view.keepScreenOn = player.isPlaying
        vm.pipEligible = settings.autoPip && player.isPlaying
    }

    // ---- полноэкранный режим, ориентация, яркость
    DisposableEffect(Unit) {
        val window = activity?.window
        val controller = if (window != null) WindowCompat.getInsetsController(window, view) else null
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        if (settingsState.value.autoLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.applyBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
            view.keepScreenOn = false
            vm.pipEligible = false
        }
    }

    // ---- пауза, когда приложение ушло в фон (но не в PiP); освобождение плеера
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !vm.inPip) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(player) {
        onDispose {
            if (settingsState.value.rememberPosition) {
                vm.saveResume(sourceState.value.key, player.timeMs(), player.lengthMs)
            }
            player.release()
        }
    }

    BackHandler {
        when {
            panel != null -> panel = null
            locked -> { controlsVisible = true; interaction++ }
            else -> onExit()
        }
    }

    // ================================================================= картинка
    AndroidView(
        factory = { c -> VLCVideoLayout(c).also { player.attach(it) } },
        modifier = Modifier.fillMaxSize(),
    )

    val inPip = vm.inPip

    // ================================================================= жесты
    if (!inPip) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(locked, settings.holdSpeed) {
                    detectTapGestures(
                        onPress = {
                            if (!locked && settings.holdSpeed) {
                                val job = scope.launch {
                                    delay(450)
                                    boost = true
                                    player.setRate(2f)
                                    showHud(Hud(Icons.Rounded.FastForward, "2×"))
                                }
                                tryAwaitRelease()
                                job.cancel()
                                if (boost) {
                                    boost = false
                                    player.setRate(speed)
                                }
                            }
                        },
                        onTap = {
                            controlsVisible = !controlsVisible
                            interaction++
                        },
                        onDoubleTap = { offset ->
                            if (!locked) {
                                val third = size.width / 3f
                                when {
                                    offset.x < third -> {
                                        player.seekBy(-10_000L)
                                        showHud(Hud(Icons.Rounded.FastRewind, "−10 s"))
                                    }
                                    offset.x > 2 * third -> {
                                        player.seekBy(10_000L)
                                        showHud(Hud(Icons.Rounded.FastForward, "+10 s"))
                                    }
                                    else -> player.togglePlay()
                                }
                            }
                        },
                    )
                }
                .pointerInput(locked, settings.gestures) {
                    if (locked || !settings.gestures) return@pointerInput
                    var axis = 0 // 1 — горизонталь (перемотка), 2 — вертикаль (яркость/громкость)
                    var seekDelta = 0L
                    var leftSide = true
                    detectDragGestures(
                        onDragStart = { offset ->
                            axis = 0
                            seekDelta = 0L
                            leftSide = offset.x < size.width / 2f
                        },
                        onDragEnd = {
                            if (axis == 1 && player.lengthMs > 0) {
                                player.seekTo((position + seekDelta).coerceIn(0L, player.lengthMs))
                            }
                            axis = 0
                        },
                        onDragCancel = { axis = 0 },
                        onDrag = { change, amount ->
                            if (axis == 0) axis = if (abs(amount.x) > abs(amount.y)) 1 else 2
                            change.consume()
                            if (axis == 1) {
                                seekDelta += (amount.x / size.width * 120_000f).toLong()
                                val target = (position + seekDelta).coerceIn(0L, maxOf(player.lengthMs, 0L))
                                val sign = if (seekDelta >= 0) "+" else "−"
                                showHud(
                                    Hud(
                                        if (seekDelta >= 0) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                                        sign + (abs(seekDelta) / 1000) + " s  •  " + timeText(target),
                                    )
                                )
                            } else {
                                val d = -amount.y / size.height * 1.2f
                                if (leftSide) {
                                    brightness = (brightness + d).coerceIn(0.02f, 1f)
                                    activity?.applyBrightness(brightness)
                                    showHud(Hud(Icons.Rounded.BrightnessMedium, "${(brightness * 100).roundToInt()}%", brightness))
                                } else {
                                    volume = (volume + d).coerceIn(0f, 1f)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * maxVolume).roundToInt(), 0)
                                    showHud(Hud(Icons.Rounded.VolumeUp, "${(volume * 100).roundToInt()}%", volume))
                                }
                            }
                        },
                    )
                }
        )
    }

    // ================================================================= индикаторы
    if (player.buffering && !player.failed && !inPip && !(controlsVisible && !locked)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(48.dp), color = Color.White)
        }
    }

    hud?.let { h ->
        Box(Modifier.fillMaxSize().padding(top = 56.dp), contentAlignment = Alignment.TopCenter) {
            GlassSurface(
                shape = RoundedCornerShape(50),
                tint = Color.Black.copy(alpha = 0.3f),
                elevation = 8.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(h.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    if (h.progress != null) {
                        Box(
                            Modifier
                                .size(width = 110.dp, height = 5.dp)
                                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(h.progress.coerceIn(0f, 1f))
                                    .height(5.dp)
                                    .background(Color.White, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                    Text(h.text, color = Color.White, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }

    // ================================================================= панель управления
    val showControls = controlsVisible && !locked && !inPip && !player.failed && panel == null
    AnimatedVisibility(
        visible = showControls,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(220)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize()) {
            // ---- верх
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .displayCutoutPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassButton(Icons.AutoMirrored.Rounded.ArrowBack, onClick = onExit, size = 46.dp, iconSize = 22.dp)
                GlassSurface(
                    Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                    tint = Color.Black.copy(alpha = 0.18f),
                    elevation = 6.dp,
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            source.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (request.items.size > 1) {
                            Text(
                                "${index + 1}/${request.items.size}",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                GlassButton(
                    Icons.Rounded.Subtitles,
                    onClick = { panel = Panel.TRACKS },
                    size = 46.dp,
                    iconSize = 22.dp,
                    active = player.currentSpu >= 0,
                )
            }

            // ---- центр
            Row(
                Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (request.items.size > 1) {
                    GlassButton(
                        Icons.Rounded.SkipPrevious,
                        onClick = { if (index > 0) switchTo(index - 1) },
                        size = 54.dp,
                        enabled = index > 0,
                    )
                }
                GlassButton(Icons.Rounded.Replay10, onClick = { player.seekBy(-10_000L); interaction++ }, size = 58.dp, iconSize = 28.dp)
                GlassSurface(
                    Modifier.size(94.dp).glassClick { player.togglePlay(); interaction++ },
                    shape = CircleShape,
                    tint = g.accent.copy(alpha = 0.32f),
                    strength = 1.3f,
                    elevation = 16.dp,
                ) {
                    Crossfade(
                        targetState = player.isPlaying,
                        modifier = Modifier.align(Alignment.Center),
                        label = "playpause",
                    ) { playing ->
                        Icon(
                            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    if (player.buffering) {
                        CircularProgressIndicator(
                            Modifier.align(Alignment.Center).size(84.dp),
                            color = Color.White.copy(alpha = 0.85f),
                            strokeWidth = 3.dp,
                        )
                    }
                }
                GlassButton(Icons.Rounded.Forward10, onClick = { player.seekBy(10_000L); interaction++ }, size = 58.dp, iconSize = 28.dp)
                if (request.items.size > 1) {
                    GlassButton(
                        Icons.Rounded.SkipNext,
                        onClick = { if (index < request.items.lastIndex) switchTo(index + 1) },
                        size = 54.dp,
                        enabled = index < request.items.lastIndex,
                    )
                }
            }

            // ---- низ
            val len = player.lengthMs
            val shownPos = if (dragging) (dragFraction * len).toLong() else position
            GlassSurface(
                Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .displayCutoutPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(34.dp),
                strength = 1.2f,
                elevation = 16.dp,
                base = g.menuBase.scaleAlpha(0.5f),
            ) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(timeText(shownPos), color = Color.White, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (len > 0) "-" + timeText((len - shownPos).coerceAtLeast(0L)) else "LIVE",
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    GlassSeekBar(
                        fraction = if (len > 0) (shownPos.toFloat() / len.toFloat()) else 0f,
                        enabled = len > 0,
                        onChange = {
                            dragging = true
                            dragFraction = it
                            interaction++
                        },
                        onChangeFinished = {
                            if (len > 0) player.seekTo((it * len).toLong())
                            position = (it * len).toLong()
                            dragging = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassButton(Icons.Rounded.Lock, onClick = { locked = true; controlsVisible = true; interaction++ }, size = 44.dp, iconSize = 21.dp)
                        GlassChip(speedText(speed), speed != 1f, { panel = Panel.SPEED }, icon = Icons.Rounded.Speed)
                        GlassButton(Icons.Rounded.AspectRatio, onClick = { panel = Panel.ASPECT }, size = 44.dp, iconSize = 21.dp, active = aspect != Aspect.FIT)
                        GlassButton(Icons.Rounded.Audiotrack, onClick = { panel = Panel.TRACKS }, size = 44.dp, iconSize = 21.dp)
                        GlassButton(
                            Icons.Rounded.PictureInPictureAlt,
                            onClick = { (activity as? MainActivity)?.enterPip() },
                            size = 44.dp,
                            iconSize = 21.dp,
                        )
                        GlassButton(
                            Icons.Rounded.ScreenRotation,
                            onClick = {
                                landscape = !landscape
                                activity?.requestedOrientation =
                                    if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                interaction++
                            },
                            size = 44.dp,
                            iconSize = 21.dp,
                        )
                    }
                }
            }
        }
    }

    // ---- блокировка: остаётся только кнопка «разблокировать»
    AnimatedVisibility(
        visible = locked && controlsVisible && !inPip,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize().displayCutoutPadding().navigationBarsPadding().padding(28.dp), contentAlignment = Alignment.CenterStart) {
            GlassButton(
                Icons.Rounded.LockOpen,
                onClick = { locked = false; interaction++ },
                size = 56.dp,
                iconSize = 26.dp,
                active = true,
            )
        }
    }

    // ================================================================= панели
    PlayerPanel(visible = panel == Panel.TRACKS && !inPip, onDismiss = { panel = null }, title = stringResource(R.string.panel_tracks)) {
        TracksPanel(player, ctx)
    }
    PlayerPanel(visible = panel == Panel.SPEED && !inPip, onDismiss = { panel = null }, title = stringResource(R.string.panel_speed)) {
        Text(
            speedText(speed),
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (s in SPEED_VALUES) {
                GlassChip(speedText(s), speed == s, {
                    speed = s
                    player.setRate(s)
                })
            }
        }
    }
    PlayerPanel(visible = panel == Panel.ASPECT && !inPip, onDismiss = { panel = null }, title = stringResource(R.string.panel_aspect)) {
        for (a in Aspect.values()) {
            GlassMenuItem(
                text = aspectName(a),
                selected = aspect == a,
                onClick = {
                    aspect = a
                    player.setAspect(a)
                },
            )
        }
    }

    // ================================================================= ошибка воспроизведения
    if (player.failed && !inPip) {
        Box(Modifier.fillMaxSize()) {
            ErrorPane(onRetry = { retry++ }, onExit = onExit)
        }
    }
}

// ---------------------------------------------------------------------- вспомогательные

/** Стеклянная панель снизу поверх видео: затемнение, закрытие по тапу вне панели. */
@Composable
private fun PlayerPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val g = DarkGlass
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(140)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 380f)) { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                GlassSurface(
                    Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .displayCutoutPadding()
                        .navigationBarsPadding()
                        .padding(12.dp)
                        .pointerInput(Unit) { detectTapGestures { } },
                    shape = RoundedCornerShape(34.dp),
                    strength = 1.3f,
                    elevation = 20.dp,
                    base = g.menuBase,
                ) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun TracksPanel(player: PrismPlayer, ctx: Context) {
    var tab by remember { mutableIntStateOf(0) } // 0 — субтитры, 1 — аудио
    val scope = rememberCoroutineScope()
    val offText = stringResource(R.string.track_off)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                copySubtitleToCache(ctx, uri)?.let { player.addSubtitle(it) }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassChip(stringResource(R.string.tab_subtitles), tab == 0, { tab = 0 }, icon = Icons.Rounded.Subtitles)
        GlassChip(stringResource(R.string.tab_audio), tab == 1, { tab = 1 }, icon = Icons.Rounded.Audiotrack)
    }

    val tracks = if (tab == 0) player.spuTracks else player.audioTracks
    val current = if (tab == 0) player.currentSpu else player.currentAudio
    Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
        if (tracks.isEmpty()) {
            Text(
                stringResource(R.string.no_tracks),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
        for (t in tracks) {
            val label = if (t.id == -1) offText else t.name
            GlassMenuItem(
                text = label,
                selected = t.id == current,
                onClick = { if (tab == 0) player.selectSpu(t.id) else player.selectAudio(t.id) },
            )
        }
    }

    if (tab == 0) {
        GlassChip(
            stringResource(R.string.add_subtitle),
            false,
            { picker.launch(arrayOf("*/*")) },
            icon = Icons.Rounded.Add,
        )
    }

    // задержка субтитров или звука
    val delayMs = if (tab == 0) player.spuDelayMs else player.audioDelayMs
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(if (tab == 0) R.string.sub_delay else R.string.audio_delay),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        GlassButton(
            Icons.Rounded.Remove,
            onClick = { if (tab == 0) player.shiftSubtitleDelay(-500) else player.shiftAudioDelay(-100) },
            size = 40.dp,
            iconSize = 20.dp,
        )
        Text(
            "%+.1f s".format(delayMs / 1000.0),
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
        )
        GlassButton(
            Icons.Rounded.Add,
            onClick = { if (tab == 0) player.shiftSubtitleDelay(500) else player.shiftAudioDelay(100) },
            size = 40.dp,
            iconSize = 20.dp,
        )
    }
}

/** libVLC читает субтитры по пути к файлу, поэтому выбранный через системный диалог файл копируем в кэш. */
private fun copySubtitleToCache(ctx: Context, uri: Uri): File? {
    return try {
        var name = "subtitle.srt"
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) c.getString(i)?.let { name = it }
            }
        }
        val dir = File(ctx.cacheDir, "subs").apply { mkdirs() }
        val out = File(dir, name)
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        out
    } catch (e: Exception) {
        null
    }
}
