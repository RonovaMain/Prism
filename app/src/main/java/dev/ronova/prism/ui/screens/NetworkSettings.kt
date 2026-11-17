package dev.ronova.prism.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronova.prism.R
import dev.ronova.prism.data.Aspect
import dev.ronova.prism.data.HwMode
import dev.ronova.prism.data.Keys
import dev.ronova.prism.data.PlayerOrientation
import dev.ronova.prism.data.ThemeMode
import dev.ronova.prism.ui.PrismViewModel
import dev.ronova.prism.ui.glass.GlassChip
import dev.ronova.prism.ui.glass.GlassSurface
import dev.ronova.prism.ui.glass.GlassSwitch
import dev.ronova.prism.ui.glass.GlassTextField
import dev.ronova.prism.ui.glass.PrismLogo
import dev.ronova.prism.ui.glass.glassClick
import dev.ronova.prism.ui.theme.LocalGlass
import dev.ronova.prism.ui.theme.prismBrush

private const val AUTHOR_NAME = "RonovaDev"
private const val GITHUB_USER = "RonovaMain"
private const val GITHUB_URL = "https://github.com/RonovaMain"

@Composable
private fun ScreenTitle(text: String) {
    val g = LocalGlass.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PrismLogo(40.dp)
        Text(text, style = MaterialTheme.typography.headlineMedium, color = g.text)
    }
}

// ---------------------------------------------------------------------- сеть

@Composable
internal fun NetworkTab(vm: PrismViewModel) {
    val g = LocalGlass.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var url by remember { mutableStateOf("") }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.playUri(uri)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = statusTop + 12.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenTitle(stringResource(R.string.tab_network)) }
        item {
            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), elevation = 8.dp) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.open_url), style = MaterialTheme.typography.titleLarge, color = g.text)
                    GlassTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = "https://…  rtsp://…  rtmp://…",
                        leading = Icons.Rounded.Link,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { vm.playUrl(url) }),
                    )
                    GlassChip(
                        text = stringResource(R.string.play),
                        selected = true,
                        icon = Icons.Rounded.PlayArrow,
                        onClick = { vm.playUrl(url) },
                    )
                    Text(
                        stringResource(R.string.protocols_hint),
                        color = g.textDim,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item {
            GlassSurface(
                Modifier.fillMaxWidth().glassClick {
                    picker.launch(arrayOf("video/*", "application/x-matroska", "application/octet-stream"))
                },
                shape = RoundedCornerShape(28.dp),
                elevation = 5.dp,
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = g.text)
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(R.string.open_file), color = g.text, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (settings.recentUrls.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = g.textDim, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.recent), color = g.textDim, style = MaterialTheme.typography.titleSmall)
                }
            }
            items(settings.recentUrls, key = { "u:$it" }) { u ->
                GlassSurface(
                    Modifier.fillMaxWidth().glassClick { vm.playUrl(u) },
                    shape = RoundedCornerShape(24.dp),
                    elevation = 3.dp,
                ) {
                    Row(Modifier.padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            u,
                            color = g.text,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = null,
                            tint = g.textDim,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                                .glassClick { vm.removeRecentUrl(u) },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- настройки

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val g = LocalGlass.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            color = g.textDim,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 8.dp),
        )
        GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), elevation = 6.dp) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = g.text, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, color = g.textDim, style = MaterialTheme.typography.bodySmall)
            }
        }
        GlassSwitch(checked, onChange)
    }
}

@Composable
private fun ChipGroup(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    val g = LocalGlass.current
    Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = g.text, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) Text(subtitle, color = g.textDim, style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
internal fun SettingsTab(vm: PrismViewModel) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = statusTop + 12.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenTitle(stringResource(R.string.tab_settings)) }

        item {
            SettingsSection(stringResource(R.string.set_appearance)) {
                ChipGroup(stringResource(R.string.set_theme)) {
                    GlassChip(stringResource(R.string.theme_dark), s.themeMode == ThemeMode.DARK, { vm.setPref(Keys.THEME, ThemeMode.DARK.name) })
                    GlassChip(stringResource(R.string.theme_light), s.themeMode == ThemeMode.LIGHT, { vm.setPref(Keys.THEME, ThemeMode.LIGHT.name) })
                    GlassChip(stringResource(R.string.theme_system), s.themeMode == ThemeMode.SYSTEM, { vm.setPref(Keys.THEME, ThemeMode.SYSTEM.name) })
                }
            }
        }

        item {
            SettingsSection(stringResource(R.string.set_playback)) {
                SwitchRow(stringResource(R.string.set_remember), stringResource(R.string.set_remember_sub), s.rememberPosition) {
                    vm.setPref(Keys.REMEMBER, it)
                }
                SwitchRow(stringResource(R.string.set_autonext), null, s.autoPlayNext) { vm.setPref(Keys.AUTONEXT, it) }
                SwitchRow(stringResource(R.string.set_gestures), stringResource(R.string.set_gestures_sub), s.gestures) {
                    vm.setPref(Keys.GESTURES, it)
                }
                SwitchRow(stringResource(R.string.set_hold), stringResource(R.string.set_hold_sub), s.holdSpeed) {
                    vm.setPref(Keys.HOLD, it)
                }
                ChipGroup(stringResource(R.string.set_orientation), stringResource(R.string.set_orientation_sub)) {
                    GlassChip(stringResource(R.string.orient_auto), s.orientation == PlayerOrientation.AUTO, { vm.setPref(Keys.ORIENTATION, PlayerOrientation.AUTO.name) })
                    GlassChip(stringResource(R.string.orient_landscape), s.orientation == PlayerOrientation.LANDSCAPE, { vm.setPref(Keys.ORIENTATION, PlayerOrientation.LANDSCAPE.name) })
                    GlassChip(stringResource(R.string.orient_portrait), s.orientation == PlayerOrientation.PORTRAIT, { vm.setPref(Keys.ORIENTATION, PlayerOrientation.PORTRAIT.name) })
                }
                SwitchRow(stringResource(R.string.set_pip), stringResource(R.string.set_pip_sub), s.autoPip) { vm.setPref(Keys.PIP, it) }
                ChipGroup(stringResource(R.string.set_default_aspect)) {
                    for (a in Aspect.values()) {
                        GlassChip(aspectName(a), s.defaultAspect == a, { vm.setPref(Keys.ASPECT, a.name) })
                    }
                }
            }
        }

        item {
            SettingsSection(stringResource(R.string.set_decoding)) {
                ChipGroup(stringResource(R.string.set_hw), stringResource(R.string.set_hw_sub)) {
                    GlassChip(stringResource(R.string.hw_auto), s.hwMode == HwMode.AUTO, { vm.setPref(Keys.HW, HwMode.AUTO.name) })
                    GlassChip(stringResource(R.string.hw_off), s.hwMode == HwMode.OFF, { vm.setPref(Keys.HW, HwMode.OFF.name) })
                    GlassChip(stringResource(R.string.hw_force), s.hwMode == HwMode.FORCE, { vm.setPref(Keys.HW, HwMode.FORCE.name) })
                }
                ChipGroup(stringResource(R.string.set_subenc), stringResource(R.string.set_subenc_sub)) {
                    val options = listOf(
                        "" to stringResource(R.string.enc_auto),
                        "UTF-8" to "UTF-8",
                        "Windows-1251" to "Windows-1251",
                        "KOI8-R" to "KOI8-R",
                        "ISO-8859-1" to "Latin-1",
                    )
                    for ((value, label) in options) {
                        GlassChip(label, s.subtitleEncoding == value, { vm.setPref(Keys.SUB_ENC, value) })
                    }
                }
                ChipGroup(stringResource(R.string.set_caching), stringResource(R.string.set_caching_sub)) {
                    for (ms in listOf(500, 1500, 3000, 6000)) {
                        GlassChip("$ms ms", s.networkCaching == ms, { vm.setPref(Keys.CACHING, ms) })
                    }
                }
            }
        }

        item {
            SettingsSection(stringResource(R.string.set_data)) {
                Row(Modifier.padding(vertical = 4.dp)) {
                    GlassChip(stringResource(R.string.clear_resume), false, { vm.clearResume() }, icon = Icons.Rounded.History)
                }
            }
        }

        item {
            SettingsSection(stringResource(R.string.set_about)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    PrismLogo(96.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Prism",
                        style = MaterialTheme.typography.headlineMedium.merge(androidx.compose.ui.text.TextStyle(brush = prismBrush())),
                    )
                    if (version.isNotEmpty()) {
                        Text("v$version", color = g.textDim, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.about_tagline),
                        color = g.text,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                AboutRow(Icons.Rounded.Person, stringResource(R.string.about_author), AUTHOR_NAME) { openUrl(ctx, GITHUB_URL) }
                AboutRow(Icons.Rounded.Code, "GitHub", "github.com/$GITHUB_USER") { openUrl(ctx, GITHUB_URL) }
                Text(
                    stringResource(R.string.about_engine),
                    color = g.textDim,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun AboutRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, onClick: () -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier.fillMaxWidth().glassClick(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = g.textDim)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = g.textDim, style = MaterialTheme.typography.bodySmall)
            Text(value, color = g.text, style = MaterialTheme.typography.bodyLarge)
        }
        Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = g.textDim, modifier = Modifier.size(18.dp))
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}
