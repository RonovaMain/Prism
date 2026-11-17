package dev.ronova.prism.data

import android.net.Uri

enum class ThemeMode { SYSTEM, DARK, LIGHT }

/** Аппаратное декодирование: авто / выключено / принудительно. */
enum class HwMode { AUTO, OFF, FORCE }

/** Формат кадра. */
enum class Aspect { FIT, CROP, STRETCH, R16_9, R4_3, ORIGINAL }

enum class LibSort { DATE, NAME, DURATION, SIZE }

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val size: Long,
    val added: Long,
    val folder: String,
    val folderId: Long,
    val width: Int,
    val height: Int,
) {
    val key: String get() = uri.toString()
    val title: String get() = name.substringBeforeLast('.', name)
}

data class FolderGroup(val id: Long, val name: String, val items: List<VideoItem>)

/** Что играем: ссылка, имя, ключ для «продолжить просмотр» и признак сетевого потока. */
data class PlaySource(val uri: Uri, val title: String, val key: String, val network: Boolean)

data class PlayRequest(val items: List<PlaySource>, val index: Int)

fun VideoItem.toSource(): PlaySource = PlaySource(uri, title, key, false)
