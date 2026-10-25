package id.nexrify.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import id.nexrify.music.data.auth.AuthManager
import id.nexrify.music.data.remote.NexrifyDownloader
import org.schabi.newpipe.extractor.NewPipe
import id.nexrify.music.data.settings.SettingsManager
import id.nexrify.music.viewmodel.MusicViewModel

class NexrifyApp : Application(), ImageLoaderFactory {

    /**
     * ViewModel dipegang oleh Application (bukan oleh Activity) supaya antrean, tombol
     * next/prev dari notifikasi, headset, dan widget tetap bekerja walau Activity ditutup
     * selagi musik masih berjalan di latar belakang.
     */
    val musicViewModel: MusicViewModel by lazy {
        MusicViewModel(this)
    }

    companion object {
        /** Loading screen hanya tampil sekali per proses (bukan tiap rotasi layar). */
        @Volatile
        var splashShown: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        AuthManager.init(this)
        SettingsManager.init(this)
        val downloader = NexrifyDownloader.init(this)
        NewPipe.init(downloader)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .respectCacheHeaders(false)
            .build()
    }
}
