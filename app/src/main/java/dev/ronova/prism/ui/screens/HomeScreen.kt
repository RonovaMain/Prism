package dev.ronova.prism.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import dev.ronova.prism.R
import dev.ronova.prism.data.Keys
import dev.ronova.prism.data.LibSort
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.ronova.prism.data.VideoItem
import dev.ronova.prism.ui.PrismViewModel
import dev.ronova.prism.ui.glass.GlassButton
import dev.ronova.prism.ui.glass.GlassChip
import dev.ronova.prism.ui.glass.GlassDropdown
import dev.ronova.prism.ui.glass.GlassMenuItem
import dev.ronova.prism.ui.glass.GlassSurface
import dev.ronova.prism.ui.glass.GlassTabBar
import dev.ronova.prism.ui.glass.GlassTag
import dev.ronova.prism.ui.glass.GlassTextField
import dev.ronova.prism.ui.glass.LocalBackdrop
import dev.ronova.prism.ui.glass.PrismBackground
import dev.ronova.prism.ui.glass.PrismFolderIcon
import dev.ronova.prism.ui.glass.PrismLogo
import dev.ronova.prism.ui.glass.TabItem
import dev.ronova.prism.ui.glass.glassClick
import dev.ronova.prism.ui.theme.LocalGlass
import dev.ronova.prism.ui.theme.PrismCyan
import dev.ronova.prism.ui.theme.PrismPink
import dev.ronova.prism.ui.theme.PrismViolet
import dev.ronova.prism.ui.theme.prismBrush
import dev.ronova.prism.ui.videoPermission

// ---------------------------------------------------------------------- форматирование

fun formatDuration(ms: Long): String {
    val t = (ms / 1000).coerceAtLeast(0)
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun qualityLabel(height: Int): String? = when {
    height >= 4320 -> "8K"
    height >= 2160 -> "4K"
    height >= 1440 -> "2K"
    height >= 1080 -> "1080p"
    height >= 720 -> "720p"
    height >= 480 -> "480p"
    else -> null
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1L shl 20 -> "%.0f MB".format(bytes / (1024.0 * 1024.0))
    else -> "%.0f KB".format(bytes / 1024.0)
}

// ---------------------------------------------------------------------- экран

@Composable
fun HomeScreen(vm: PrismViewModel, animate: Boolean) {
    val tabs = listOf(
        TabItem(Icons.Rounded.VideoLibrary, stringResource(R.string.tab_videos)),
        TabItem(Icons.Rounded.Folder, stringResource(R.string.tab_folders)),
        TabItem(Icons.Rounded.Public, stringResource(R.string.tab_network)),
        TabItem(Icons.Rounded.Settings, stringResource(R.string.tab_settings)),
    )

    BackHandler(enabled = vm.searchOpen || (vm.tab == 1 && vm.openFolderId != null)) {
        if (vm.searchOpen) {
            vm.searchOpen = false
            vm.query.value = ""
        } else {
            vm.openFolderId = null
        }
    }

    PrismBackground(Modifier.fillMaxSize(), animate = animate) {
        // Содержимое записывается отдельным слоем: когда карточки прокручиваются под плавающей панелью
        // вкладок, она преломляет и фон, и их. Сама панель в запись не входит (иначе стекло рисовало бы себя).
        val background = LocalBackdrop.current
        val content = rememberLayerBackdrop()
        val tabBackdrop = rememberCombinedBackdrop(background, content)
        Box(Modifier.matchParentSize().layerBackdrop(content)) {
            Crossfade(targetState = vm.tab, label = "tab") { tab ->
                when (tab) {
                    0 -> VideosTab(vm)
                    1 -> FoldersTab(vm)
                    2 -> NetworkTab(vm)
                    else -> SettingsTab(vm)
                }
            }
        }
        GlassTabBar(
            backdrop = tabBackdrop,
            items = tabs,
            selected = vm.tab,
            onSelect = {
                vm.tab = it
                if (it != 1) vm.openFolderId = null
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 14.dp),
        )
    }
}

// ---------------------------------------------------------------------- вкладка «Видео»

@Composable
private fun VideosTab(vm: PrismViewModel) {
    val displayed by vm.displayed.collectAsStateWithLifecycle()
    if (!vm.hasPermission) {
        PermissionPane(vm)
    } else {
        VideoCollection(vm = vm, items = displayed, title = null, onBack = null)
    }
}

@Composable
private fun PermissionPane(vm: PrismViewModel) {
    val g = LocalGlass.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refreshPermission()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.playUri(uri)
    }
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(36.dp), elevation = 14.dp) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PrismLogo(84.dp)
                Text(
                    stringResource(R.string.perm_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = g.text,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.perm_message),
                    color = g.textDim,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                GlassChip(
                    text = stringResource(R.string.perm_allow),
                    selected = true,
                    onClick = { launcher.launch(videoPermission()) },
                    icon = Icons.Rounded.Lock,
                )
                GlassChip(
                    text = stringResource(R.string.open_file),
                    selected = false,
                    onClick = { picker.launch(arrayOf("video/*", "application/x-matroska", "application/octet-stream")) },
                    icon = Icons.Rounded.FolderOpen,
                )
            }
        }
    }
}

/** Заголовок, поиск и сетка/список видео. Порядок плейлиста = порядок на экране. */
@Composable
private fun VideoCollection(
    vm: PrismViewModel,
    items: List<VideoItem>,
    title: String?,
    onBack: (() -> Unit)?,
) {
    val g = LocalGlass.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val allVideos by vm.videos.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val resume by vm.resume.collectAsStateWithLifecycle()
    var sortMenu by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.playUri(uri)
    }

    val continueItems = remember(allVideos, resume) {
        if (title != null) emptyList() else {
            val byKey = allVideos.associateBy { it.key }
            resume.keys.mapNotNull { byKey[it] }.take(8)
        }
    }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val grid = settings.libraryGrid

    LazyVerticalGrid(
        columns = if (grid) GridCells.Adaptive(165.dp) else GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = statusTop + 12.dp, bottom = 130.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---- шапка
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (onBack != null) {
                        GlassButton(Icons.AutoMirrored.Rounded.ArrowBack, onBack, size = 44.dp, iconSize = 22.dp)
                    } else {
                        PrismLogo(40.dp)
                    }
                    Text(
                        title ?: "Prism",
                        style = MaterialTheme.typography.headlineMedium.merge(
                            if (title == null) TextStyle(brush = prismBrush()) else TextStyle(color = g.text)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    GlassButton(
                        Icons.Rounded.Search,
                        onClick = {
                            vm.searchOpen = !vm.searchOpen
                            if (!vm.searchOpen) vm.query.value = ""
                        },
                        size = 44.dp,
                        iconSize = 22.dp,
                        active = vm.searchOpen,
                    )
                    GlassButton(
                        Icons.Rounded.FolderOpen,
                        onClick = { picker.launch(arrayOf("video/*", "application/x-matroska", "application/octet-stream")) },
                        size = 44.dp,
                        iconSize = 22.dp,
                    )
                    Box {
                        GlassButton(Icons.Rounded.Sort, onClick = { sortMenu = true }, size = 44.dp, iconSize = 22.dp)
                        GlassDropdown(expanded = sortMenu, onDismiss = { sortMenu = false }) {
                            val options = listOf(
                                LibSort.DATE to R.string.sort_date,
                                LibSort.NAME to R.string.sort_name,
                                LibSort.DURATION to R.string.sort_duration,
                                LibSort.SIZE to R.string.sort_size,
                            )
                            for ((opt, label) in options) {
                                GlassMenuItem(
                                    text = stringResource(label),
                                    selected = settings.librarySort == opt,
                                    onClick = { vm.setPref(Keys.SORT, opt.name) },
                                )
                            }
                            GlassMenuItem(
                                text = stringResource(R.string.sort_ascending),
                                selected = settings.libraryAscending,
                                onClick = { vm.setPref(Keys.ASC, !settings.libraryAscending) },
                            )
                            GlassMenuItem(
                                text = stringResource(if (grid) R.string.view_list else R.string.view_grid),
                                icon = if (grid) Icons.Rounded.ViewAgenda else Icons.Rounded.ViewModule,
                                onClick = {
                                    vm.setPref(Keys.GRID, !grid)
                                    sortMenu = false
                                },
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = vm.searchOpen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    GlassTextField(
                        value = query,
                        onValueChange = { vm.query.value = it },
                        placeholder = stringResource(R.string.search_hint),
                        leading = Icons.Rounded.Search,
                        modifier = Modifier.fillMaxWidth(),
                        trailing = {
                            if (query.isNotEmpty()) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = g.textDim,
                                    modifier = Modifier.size(20.dp).glassClick { vm.query.value = "" },
                                )
                            }
                        },
                    )
                }
                Text(
                    stringResource(R.string.videos_count, items.size),
                    color = g.textDim,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        // ---- «Продолжить просмотр»
        if (continueItems.isNotEmpty() && query.isBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.continue_watching),
                        style = MaterialTheme.typography.titleLarge,
                        color = g.text,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(continueItems, key = { "c:" + it.key }) { v ->
                            Box(Modifier.width(230.dp)) {
                                VideoCard(v, resume[v.key], showText = true) {
                                    vm.play(continueItems, continueItems.indexOf(v))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- сами видео
        if (items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(if (vm.loading) R.string.loading else R.string.nothing_found),
                        color = g.textDim,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else if (grid) {
            items(items, key = { it.key }) { v ->
                VideoCard(v, resume[v.key], showText = true) { vm.play(items, items.indexOf(v)) }
            }
        } else {
            items(items, key = { it.key }) { v ->
                VideoRow(v, resume[v.key]) { vm.play(items, items.indexOf(v)) }
            }
        }
    }
}

@Composable
private fun Thumbnail(item: VideoItem, modifier: Modifier, resumeMs: Long?, cornerRadius: Int) {
    val ctx = LocalContext.current
    val g = LocalGlass.current
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(PrismViolet.copy(alpha = 0.35f), PrismPink.copy(alpha = 0.25f), PrismCyan.copy(alpha = 0.3f))
                )
            )
            .border(1.dp, g.rimDim.copy(alpha = 0.5f), shape),
        contentAlignment = Alignment.Center,
    ) {
        PrismLogo(30.dp, Modifier.align(Alignment.Center))
        AsyncImage(
            model = ImageRequest.Builder(ctx)
                .data(item.uri)
                .videoFrameMillis((item.durationMs / 10).coerceIn(500L, 20_000L))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)), startY = 120f))
        )
        GlassTag(formatDuration(item.durationMs), Modifier.align(Alignment.BottomEnd).padding(8.dp))
        qualityLabel(item.height)?.let { GlassTag(it, Modifier.align(Alignment.TopStart).padding(8.dp)) }
        if (resumeMs != null && item.durationMs > 0) {
            val f = (resumeMs.toFloat() / item.durationMs.toFloat()).coerceIn(0.02f, 1f)
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(f)
                    .height(3.dp)
                    .background(prismBrush())
            )
        }
    }
}

@Composable
private fun VideoCard(item: VideoItem, resumeMs: Long?, showText: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    Column(Modifier.glassClick(onClick = onClick), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Thumbnail(item, Modifier.fillMaxWidth().aspectRatio(16f / 9f), resumeMs, 22)
        if (showText) {
            Column(Modifier.padding(horizontal = 4.dp)) {
                Text(
                    item.title,
                    color = g.text,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatSize(item.size),
                    color = g.textDim,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun VideoRow(item: VideoItem, resumeMs: Long?, onClick: () -> Unit) {
    val g = LocalGlass.current
    GlassSurface(
        Modifier.fillMaxWidth().glassClick(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        elevation = 4.dp,
        refraction = false,
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(item, Modifier.width(132.dp).aspectRatio(16f / 9f), resumeMs, 16)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    color = g.text,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(formatSize(item.size), item.folder).joinToString("  •  "),
                    color = g.textDim,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = g.textDim)
        }
    }
}

// ---------------------------------------------------------------------- вкладка «Папки»

@Composable
private fun FoldersTab(vm: PrismViewModel) {
    val g = LocalGlass.current
    val videos by vm.videos.collectAsStateWithLifecycle()
    val displayed by vm.displayed.collectAsStateWithLifecycle()
    val folderId = vm.openFolderId

    if (!vm.hasPermission) {
        PermissionPane(vm)
        return
    }
    val groups = remember(videos) {
        videos.groupBy { it.folderId }
            .map { (id, list) -> Triple(id, list.first().folder, list) }
            .sortedBy { it.second.lowercase() }
    }
    if (folderId != null) {
        val name = groups.firstOrNull { it.first == folderId }?.second ?: ""
        VideoCollection(
            vm = vm,
            items = displayed.filter { it.folderId == folderId },
            title = name,
            onBack = { vm.openFolderId = null },
        )
        return
    }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = statusTop + 12.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrismLogo(40.dp)
                Text(
                    stringResource(R.string.tab_folders),
                    style = MaterialTheme.typography.headlineMedium,
                    color = g.text,
                )
            }
        }
        items(groups, key = { it.first }) { (id, name, list) ->
            GlassSurface(
                Modifier.fillMaxWidth().glassClick { vm.openFolderId = id },
                shape = RoundedCornerShape(28.dp),
                elevation = 5.dp,
                refraction = false,
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrismFolderIcon(46.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, color = g.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(R.string.videos_count, list.size) + "  •  " + formatSize(list.sumOf { it.size }),
                            color = g.textDim,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
