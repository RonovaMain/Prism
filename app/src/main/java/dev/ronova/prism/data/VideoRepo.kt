package dev.ronova.prism.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/** Загружает видео из медиатеки телефона (MediaStore). Нужно разрешение на чтение видео. */
object VideoRepo {

    fun load(context: Context): List<VideoItem> {
        val list = ArrayList<VideoItem>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
        )
        try {
            context.contentResolver.query(
                collection, projection, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC",
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val iName = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val iSize = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val iAdded = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val iFolder = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val iFolderId = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                val iW = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val iH = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    list.add(
                        VideoItem(
                            id = id,
                            uri = ContentUris.withAppendedId(collection, id),
                            name = c.getString(iName) ?: "video",
                            durationMs = c.getLong(iDur),
                            size = c.getLong(iSize),
                            added = c.getLong(iAdded),
                            folder = c.getString(iFolder) ?: "—",
                            folderId = c.getLong(iFolderId),
                            width = c.getInt(iW),
                            height = c.getInt(iH),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // нет разрешения или медиатека недоступна — возвращаем то, что успели прочитать
        }
        return list
    }
}
