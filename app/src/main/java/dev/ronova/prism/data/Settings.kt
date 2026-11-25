package dev.ronova.prism.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prism_settings")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val hwMode: HwMode = HwMode.AUTO,
    val rememberPosition: Boolean = true,
    val autoPlayNext: Boolean = true,
    val gestures: Boolean = true,
    val holdSpeed: Boolean = true,
    val autoPip: Boolean = true,
    /** Тени под стеклом: в интерфейсе приложения включены, в плеере по умолчанию выключены. */
    val uiShadows: Boolean = true,
    val playerShadows: Boolean = false,
    val glassMode: GlassMode = GlassMode.LITE,
    val glassOverVideo: Boolean = true,
    val orientation: PlayerOrientation = PlayerOrientation.AUTO,
    val subtitleEncoding: String = "",
    val defaultAspect: Aspect = Aspect.FIT,
    val networkCaching: Int = 1500,
    val librarySort: LibSort = LibSort.DATE,
    val libraryAscending: Boolean = false,
    val libraryGrid: Boolean = true,
    val recentUrls: List<String> = emptyList(),
)

object Keys {
    val THEME = stringPreferencesKey("theme")
    val HW = stringPreferencesKey("hw")
    val REMEMBER = booleanPreferencesKey("remember_position")
    val AUTONEXT = booleanPreferencesKey("auto_next")
    val GESTURES = booleanPreferencesKey("gestures")
    val HOLD = booleanPreferencesKey("hold_speed")
    val PIP = booleanPreferencesKey("auto_pip")
    val UI_SHADOWS = booleanPreferencesKey("ui_shadows")
    val PLAYER_SHADOWS = booleanPreferencesKey("player_shadows")
    val GLASS_MODE = stringPreferencesKey("glass_mode")
    val GLASS_VIDEO = booleanPreferencesKey("glass_over_video")
    val ORIENTATION = stringPreferencesKey("player_orientation")
    val SUB_ENC = stringPreferencesKey("subtitle_encoding")
    val ASPECT = stringPreferencesKey("default_aspect")
    val CACHING = intPreferencesKey("network_caching")
    val SORT = stringPreferencesKey("lib_sort")
    val ASC = booleanPreferencesKey("lib_asc")
    val GRID = booleanPreferencesKey("lib_grid")
    val RECENT = stringPreferencesKey("recent_urls")
    val RESUME = stringPreferencesKey("resume")
}

private inline fun <reified E : Enum<E>> parseEnum(s: String?, default: E): E =
    runCatching { enumValueOf<E>(s ?: default.name) }.getOrDefault(default)

/** key -> (позиция в мс, время записи) */
private fun parseResume(s: String?): LinkedHashMap<String, Pair<Long, Long>> {
    val map = LinkedHashMap<String, Pair<Long, Long>>()
    if (s.isNullOrEmpty()) return map
    for (line in s.split('\n')) {
        val parts = line.split('\t')
        if (parts.size == 3) {
            val pos = parts[1].toLongOrNull() ?: continue
            val ts = parts[2].toLongOrNull() ?: continue
            map[parts[0]] = pos to ts
        }
    }
    return map
}

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
            AppSettings(
                themeMode = parseEnum(p[Keys.THEME], ThemeMode.DARK),
                hwMode = parseEnum(p[Keys.HW], HwMode.AUTO),
                rememberPosition = p[Keys.REMEMBER] ?: true,
                autoPlayNext = p[Keys.AUTONEXT] ?: true,
                gestures = p[Keys.GESTURES] ?: true,
                holdSpeed = p[Keys.HOLD] ?: true,
                autoPip = p[Keys.PIP] ?: true,
                uiShadows = p[Keys.UI_SHADOWS] ?: true,
                playerShadows = p[Keys.PLAYER_SHADOWS] ?: false,
                glassMode = parseEnum(p[Keys.GLASS_MODE], GlassMode.LITE),
                glassOverVideo = p[Keys.GLASS_VIDEO] ?: true,
                orientation = parseEnum(p[Keys.ORIENTATION], PlayerOrientation.AUTO),
                subtitleEncoding = p[Keys.SUB_ENC] ?: "",
                defaultAspect = parseEnum(p[Keys.ASPECT], Aspect.FIT),
                networkCaching = p[Keys.CACHING] ?: 1500,
                librarySort = parseEnum(p[Keys.SORT], LibSort.DATE),
                libraryAscending = p[Keys.ASC] ?: false,
                libraryGrid = p[Keys.GRID] ?: true,
                recentUrls = (p[Keys.RECENT] ?: "").split('\n').filter { it.isNotBlank() },
            )
        }

    /** Ключ видео -> позиция, на которой остановились. */
    val resume: Flow<Map<String, Long>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { p -> parseResume(p[Keys.RESUME]).mapValues { it.value.first } }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun saveResume(key: String, positionMs: Long, lengthMs: Long) {
        context.dataStore.edit { p ->
            val map = parseResume(p[Keys.RESUME])
            val finished = lengthMs > 0 && positionMs > lengthMs - 8_000
            if (positionMs < 5_000 || finished) {
                map.remove(key)
            } else {
                map[key] = positionMs to System.currentTimeMillis()
            }
            val trimmed = map.entries.sortedByDescending { it.value.second }.take(300)
            p[Keys.RESUME] = trimmed.joinToString("\n") { "${it.key}\t${it.value.first}\t${it.value.second}" }
        }
    }

    suspend fun clearResume() {
        context.dataStore.edit { it[Keys.RESUME] = "" }
    }

    suspend fun addRecentUrl(url: String) {
        context.dataStore.edit { p ->
            val cur = (p[Keys.RECENT] ?: "").split('\n').filter { it.isNotBlank() && it != url }
            p[Keys.RECENT] = (listOf(url) + cur).take(20).joinToString("\n")
        }
    }

    suspend fun removeRecentUrl(url: String) {
        context.dataStore.edit { p ->
            val cur = (p[Keys.RECENT] ?: "").split('\n').filter { it.isNotBlank() && it != url }
            p[Keys.RECENT] = cur.joinToString("\n")
        }
    }
}
