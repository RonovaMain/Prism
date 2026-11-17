package dev.ronova.prism.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ronova.prism.PrismApp
import dev.ronova.prism.data.AppSettings
import dev.ronova.prism.data.LibSort
import dev.ronova.prism.data.PlayRequest
import dev.ronova.prism.data.PlaySource
import dev.ronova.prism.data.VideoItem
import dev.ronova.prism.data.VideoRepo
import dev.ronova.prism.data.toSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun videoPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE

private val NETWORK_SCHEMES = setOf("http", "https", "rtsp", "rtmp", "mms", "mmsh", "udp", "rtp", "ftp", "sftp", "smb")

fun List<VideoItem>.sortedFor(sort: LibSort, ascending: Boolean): List<VideoItem> {
    val ci = String.CASE_INSENSITIVE_ORDER
    val cmp: Comparator<VideoItem> = when (sort) {
        LibSort.DATE -> compareBy<VideoItem> { it.added }
        LibSort.NAME -> compareBy<VideoItem, String>(ci) { it.name }
        LibSort.DURATION -> compareBy<VideoItem> { it.durationMs }
        LibSort.SIZE -> compareBy<VideoItem> { it.size }
    }
    return sortedWith(if (ascending) cmp else cmp.reversed())
}

class PrismViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as PrismApp).settingsRepo

    val settings: StateFlow<AppSettings> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val resume: StateFlow<Map<String, Long>> = repo.resume.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // ---------------------------------------------------------------- медиатека
    var hasPermission by mutableStateOf(checkPermission())
        private set
    var loading by mutableStateOf(false)
        private set
    val videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val query = MutableStateFlow("")

    var tab by mutableIntStateOf(0)
    var openFolderId by mutableStateOf<Long?>(null)
    var searchOpen by mutableStateOf(false)

    /** Список в текущем порядке сортировки и с учётом поиска. */
    val displayed: StateFlow<List<VideoItem>> = combine(videos, settings, query) { list, s, q ->
        val needle = q.trim()
        val filtered = if (needle.isEmpty()) list else list.filter { it.name.contains(needle, ignoreCase = true) }
        filtered.sortedFor(s.librarySort, s.libraryAscending)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---------------------------------------------------------------- плеер
    var playRequest by mutableStateOf<PlayRequest?>(null)
        private set
    /** true, если приложение открыли ссылкой из другой программы: при выходе из плеера закрываемся. */
    var finishOnExit = false
    var inPip by mutableStateOf(false)
    var pipEligible by mutableStateOf(false)
    /** Пропорции видео для окна PiP (ширина, высота); 0 — ещё неизвестно. */
    var pipWidth by mutableIntStateOf(16)
    var pipHeight by mutableIntStateOf(9)

    init {
        if (hasPermission) loadVideos()
    }

    private fun checkPermission(): Boolean =
        ContextCompat.checkSelfPermission(getApplication(), videoPermission()) == PackageManager.PERMISSION_GRANTED

    fun refreshPermission() {
        val now = checkPermission()
        if (now != hasPermission) {
            hasPermission = now
            if (now) loadVideos()
        }
    }

    fun loadVideos() {
        if (!checkPermission()) return
        viewModelScope.launch {
            loading = true
            videos.value = withContext(Dispatchers.IO) { VideoRepo.load(getApplication()) }
            loading = false
        }
    }

    fun <T> setPref(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch { repo.set(key, value) }
    }

    fun clearResume() {
        viewModelScope.launch { repo.clearResume() }
    }

    // ---------------------------------------------------------------- запуск воспроизведения
    /** Играет список в том порядке, в котором он показан на экране. */
    fun play(items: List<VideoItem>, index: Int) {
        if (items.isEmpty()) return
        playRequest = PlayRequest(items.map { it.toSource() }, index.coerceIn(0, items.lastIndex))
    }

    fun playUrl(raw: String) {
        var url = raw.trim()
        if (url.isEmpty()) return
        if (!url.contains("://")) url = "http://$url"
        viewModelScope.launch { repo.addRecentUrl(url) }
        val uri = Uri.parse(url)
        playRequest = PlayRequest(listOf(PlaySource(uri, url.substringAfterLast('/').ifEmpty { url }, url, true)), 0)
    }

    /** Файл, выбранный в системном диалоге, или ссылка из «Открыть с помощью». */
    fun playUri(uri: Uri, external: Boolean = false) {
        val scheme = uri.scheme?.lowercase() ?: "file"
        val network = scheme in NETWORK_SCHEMES
        val title = displayName(uri)
        if (external) finishOnExit = true
        playRequest = PlayRequest(listOf(PlaySource(uri, title, uri.toString(), network)), 0)
    }

    private fun displayName(uri: Uri): String {
        if (uri.scheme == "content") {
            try {
                getApplication<Application>().contentResolver
                    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (i >= 0) c.getString(i)?.let { return it }
                        }
                    }
            } catch (_: Exception) {
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
    }

    fun closePlayer() {
        playRequest = null
        pipEligible = false
    }

    fun saveResume(key: String, positionMs: Long, lengthMs: Long) {
        viewModelScope.launch { repo.saveResume(key, positionMs, lengthMs) }
    }

    fun removeRecentUrl(url: String) {
        viewModelScope.launch { repo.removeRecentUrl(url) }
    }
}
