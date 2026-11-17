package dev.ronova.prism.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ronova.prism.data.Aspect
import dev.ronova.prism.data.HwMode
import dev.ronova.prism.data.PlaySource
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File

/**
 * Обёртка над libVLC: тот же движок, что в VLC, поэтому понимает практически любой формат
 * (MKV, AVI, WMV, FLV, TS, MP4, WebM, потоки HTTP/HLS/RTSP/RTMP, куча аудио- и видеокодеков и субтитров).
 * Состояние выставлено как Compose-state, чтобы интерфейс обновлялся сам.
 */
class PrismPlayer(
    private val context: Context,
    private val hw: HwMode,
    subtitleEncoding: String,
) {
    data class Track(val id: Int, val name: String)

    private val libVLC: LibVLC
    private val mediaPlayer: MediaPlayer
    private var pfd: ParcelFileDescriptor? = null
    private var released = false

    // ---- поверхность видео: libVLC сам не переживает её пересоздание (сворачивание, PiP, поворот)
    private val handler = Handler(Looper.getMainLooper())
    private var layout: VLCVideoLayout? = null
    private var hookedSurface: SurfaceView? = null
    private var surfaceSeen = false
    private var suspended = false
    private var savedVideoTrack = -1
    private var scale: MediaPlayer.ScaleType = MediaPlayer.ScaleType.SURFACE_BEST_FIT
    private val rebindTask = Runnable { if (!released && !suspended) recreateVideoOutput() }

    var isPlaying by mutableStateOf(false)
        private set
    var buffering by mutableStateOf(false)
        private set
    var lengthMs by mutableLongStateOf(0L)
        private set
    var failed by mutableStateOf(false)
        private set
    var endedTick by mutableIntStateOf(0)
        private set
    var audioTracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var spuTracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var currentAudio by mutableIntStateOf(-1)
        private set
    var currentSpu by mutableIntStateOf(-1)
        private set
    var audioDelayMs by mutableLongStateOf(0L)
        private set
    var spuDelayMs by mutableLongStateOf(0L)
        private set
    /** Размер картинки с учётом SAR — нужен, чтобы окно PiP имело правильные пропорции. */
    var videoWidth by mutableIntStateOf(0)
        private set
    var videoHeight by mutableIntStateOf(0)
        private set

    init {
        val options = ArrayList<String>()
        options.add("--audio-time-stretch")
        if (subtitleEncoding.isNotBlank()) options.add("--subsdec-encoding=$subtitleEncoding")
        libVLC = LibVLC(context.applicationContext, options)
        mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer.setEventListener { ev -> onEvent(ev) }
    }

    private fun onEvent(ev: MediaPlayer.Event) {
        when (ev.type) {
            MediaPlayer.Event.Opening -> buffering = true
            MediaPlayer.Event.Playing -> {
                isPlaying = true
                buffering = false
                failed = false
                refreshTracks()
            }
            MediaPlayer.Event.Paused -> isPlaying = false
            MediaPlayer.Event.Stopped -> isPlaying = false
            MediaPlayer.Event.EndReached -> {
                isPlaying = false
                endedTick += 1
            }
            MediaPlayer.Event.EncounteredError -> {
                isPlaying = false
                buffering = false
                failed = true
            }
            MediaPlayer.Event.Buffering -> buffering = ev.buffering < 100f
            MediaPlayer.Event.LengthChanged -> lengthMs = ev.lengthChanged
            MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESDeleted, MediaPlayer.Event.Vout -> refreshTracks()
            else -> {}
        }
    }

    private fun refreshTracks() {
        try {
            audioTracks = mediaPlayer.getAudioTracks()?.map { Track(it.id, it.name ?: "#${it.id}") } ?: emptyList()
            spuTracks = mediaPlayer.getSpuTracks()?.map { Track(it.id, it.name ?: "#${it.id}") } ?: emptyList()
            currentAudio = mediaPlayer.getAudioTrack()
            currentSpu = mediaPlayer.getSpuTrack()
            mediaPlayer.getCurrentVideoTrack()?.let { vt ->
                if (vt.width > 0 && vt.height > 0) {
                    val sar = if (vt.sarNum > 0 && vt.sarDen > 0) vt.sarNum.toFloat() / vt.sarDen else 1f
                    videoWidth = (vt.width * sar).toInt()
                    videoHeight = vt.height
                }
            }
        } catch (_: Exception) {
        }
    }

    // ---------------------------------------------------------------- экран и запуск

    fun attach(l: VLCVideoLayout) {
        layout = l
        mediaPlayer.attachViews(l, null, true, false)
        hookSurface()
    }

    private fun findSurface(v: View?): SurfaceView? {
        if (v is SurfaceView) return v
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) findSurface(v.getChildAt(i))?.let { return it }
        }
        return null
    }

    /** Следим за пересозданием поверхности: после этого видеовыход libVLC нужно создать заново. */
    private fun hookSurface() {
        val sv = findSurface(layout) ?: return
        if (sv === hookedSurface) return
        hookedSurface = sv
        sv.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if (surfaceSeen && !suspended) {
                    handler.removeCallbacks(rebindTask)
                    handler.postDelayed(rebindTask, 150)
                }
                surfaceSeen = true
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })
    }

    private fun firstVideoTrackId(): Int = try {
        mediaPlayer.getVideoTracks()?.firstOrNull { it.id >= 0 }?.id ?: -1
    } catch (_: Exception) {
        -1
    }

    /** Выключаем и снова включаем видеодорожку: libVLC создаёт видеовыход заново на актуальной поверхности. */
    private fun recreateVideoOutput() {
        try {
            val current = mediaPlayer.getVideoTrack()
            val target = if (current >= 0) current else firstVideoTrackId()
            if (target < 0) return
            mediaPlayer.setVideoTrack(-1)
            mediaPlayer.setVideoTrack(target)
            mediaPlayer.setVideoScale(scale)
            if (!mediaPlayer.isPlaying) {
                val t = mediaPlayer.getTime()
                if (t > 0) mediaPlayer.setTime(t) // чтобы на паузе сразу появился кадр
            }
        } catch (_: Exception) {
        }
    }

    /** Приложение ушло в фон: ставим на паузу и отцепляем поверхность (как делает VLC). */
    fun suspendVideo() {
        if (suspended || released) return
        suspended = true
        handler.removeCallbacks(rebindTask)
        try {
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
        } catch (_: Exception) {
        }
        try {
            savedVideoTrack = mediaPlayer.getVideoTrack()
            mediaPlayer.setVideoTrack(-1)
        } catch (_: Exception) {
        }
        try {
            mediaPlayer.detachViews()
        } catch (_: Exception) {
        }
    }

    /** Возврат в приложение: заново подключаем поверхность и включаем видеодорожку. */
    fun resumeVideo() {
        if (!suspended || released) return
        val l = layout ?: return
        suspended = false
        try {
            mediaPlayer.attachViews(l, null, true, false)
        } catch (_: Exception) {
        }
        hookSurface()
        try {
            val target = if (savedVideoTrack >= 0) savedVideoTrack else firstVideoTrackId()
            if (target >= 0) mediaPlayer.setVideoTrack(target)
            mediaPlayer.setVideoScale(scale)
        } catch (_: Exception) {
        }
        handler.removeCallbacks(rebindTask)
        handler.postDelayed(rebindTask, 250)
    }

    /** Окно сменило режим (PiP ↔ полный экран): обновляем размер и пересоздаём видеовыход после анимации. */
    fun onWindowModeChanged() {
        if (released) return
        layout?.post {
            try {
                layout?.let { mediaPlayer.getVLCVout().setWindowSize(it.width, it.height) }
                mediaPlayer.setVideoScale(scale)
            } catch (_: Exception) {
            }
        }
        handler.removeCallbacks(rebindTask)
        handler.postDelayed(rebindTask, 450)
    }

    fun open(source: PlaySource, startMs: Long, networkCachingMs: Int) {
        failed = false
        buffering = true
        lengthMs = 0L
        audioTracks = emptyList()
        spuTracks = emptyList()
        audioDelayMs = 0L
        spuDelayMs = 0L
        closeFd()
        val media: Media = try {
            if (source.uri.scheme == "content") {
                val d = context.contentResolver.openFileDescriptor(source.uri, "r")
                    ?: throw IllegalStateException("Cannot open " + source.uri)
                pfd = d
                Media(libVLC, d.fileDescriptor)
            } else {
                Media(libVLC, source.uri)
            }
        } catch (e: Exception) {
            failed = true
            buffering = false
            return
        }
        media.setHWDecoderEnabled(hw != HwMode.OFF, hw == HwMode.FORCE)
        if (source.network) media.addOption(":network-caching=$networkCachingMs")
        if (startMs > 1000L) media.addOption(":start-time=${startMs / 1000.0}")
        mediaPlayer.setMedia(media)
        media.release()
        mediaPlayer.play()
    }

    private fun closeFd() {
        try {
            pfd?.close()
        } catch (_: Exception) {
        }
        pfd = null
    }

    // ---------------------------------------------------------------- управление

    fun togglePlay() {
        if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play()
    }

    fun play() = mediaPlayer.play()

    fun pause() {
        if (mediaPlayer.isPlaying) mediaPlayer.pause()
    }

    fun timeMs(): Long = try {
        mediaPlayer.getTime()
    } catch (e: Exception) {
        0L
    }

    fun seekTo(ms: Long) {
        mediaPlayer.setTime(ms.coerceAtLeast(0L))
    }

    fun seekBy(deltaMs: Long) {
        val max = if (lengthMs > 0) lengthMs else Long.MAX_VALUE
        seekTo((timeMs() + deltaMs).coerceIn(0L, max))
    }

    fun setRate(rate: Float) {
        mediaPlayer.setRate(rate)
    }

    fun setAspect(aspect: Aspect) {
        scale = when (aspect) {
            Aspect.FIT -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
            Aspect.CROP -> MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
            Aspect.STRETCH -> MediaPlayer.ScaleType.SURFACE_FILL
            Aspect.R16_9 -> MediaPlayer.ScaleType.SURFACE_16_9
            Aspect.R4_3 -> MediaPlayer.ScaleType.SURFACE_4_3
            Aspect.ORIGINAL -> MediaPlayer.ScaleType.SURFACE_ORIGINAL
        }
        try {
            mediaPlayer.setVideoScale(scale)
        } catch (_: Exception) {
        }
    }

    fun selectAudio(id: Int) {
        mediaPlayer.setAudioTrack(id)
        currentAudio = id
    }

    fun selectSpu(id: Int) {
        mediaPlayer.setSpuTrack(id)
        currentSpu = id
    }

    /** Подключает внешний файл субтитров (SRT, ASS, SSA, VTT, SUB…). */
    fun addSubtitle(file: File): Boolean = try {
        val ok = mediaPlayer.addSlave(IMedia.Slave.Type.Subtitle, Uri.fromFile(file), true)
        refreshTracks()
        ok
    } catch (e: Exception) {
        false
    }

    fun shiftSubtitleDelay(deltaMs: Long) {
        spuDelayMs += deltaMs
        mediaPlayer.setSpuDelay(spuDelayMs * 1000L)
    }

    fun shiftAudioDelay(deltaMs: Long) {
        audioDelayMs += deltaMs
        mediaPlayer.setAudioDelay(audioDelayMs * 1000L)
    }

    fun resetDelays() {
        spuDelayMs = 0L
        audioDelayMs = 0L
        mediaPlayer.setSpuDelay(0L)
        mediaPlayer.setAudioDelay(0L)
    }

    fun release() {
        if (released) return
        released = true
        handler.removeCallbacksAndMessages(null)
        try {
            mediaPlayer.setEventListener(null)
        } catch (_: Exception) {
        }
        try {
            mediaPlayer.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer.detachViews()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer.release()
        } catch (_: Exception) {
        }
        try {
            libVLC.release()
        } catch (_: Exception) {
        }
        closeFd()
    }
}
