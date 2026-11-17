package dev.ronova.prism

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import dev.ronova.prism.data.SettingsRepository

class PrismApp : Application(), ImageLoaderFactory {

    val settingsRepo: SettingsRepository by lazy { SettingsRepository(this) }

    /** Загрузчик миниатюр умеет доставать кадры из видео. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
