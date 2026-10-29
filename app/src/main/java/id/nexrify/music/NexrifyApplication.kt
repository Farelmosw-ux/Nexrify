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

class NexrifyApplication : Application(), ImageLoaderFactory, androidx.lifecycle.ViewModelStoreOwner {

    /** ViewModel musik dipegang di level aplikasi: tetap hidup selama pemutaran jalan, walau Activity ditutup. */
    override val viewModelStore: androidx.lifecycle.ViewModelStore = androidx.lifecycle.ViewModelStore()

    override fun onCreate() {
        super.onCreate()
        AuthManager.init(this)
        id.nexrify.music.data.prayer.MaghribManager.init(this)
        id.nexrify.music.data.network.NetworkMonitor.init(this)
        id.nexrify.music.data.download.OfflineManager.init(this)
        id.nexrify.music.data.settings.SettingsManager.init(this)
        val downloader = NexrifyDownloader.init(this)
        NewPipe.init(downloader)
        // Segarkan nama dan foto akun Google/YouTube tiap aplikasi dibuka
        AuthManager.getInstance().refreshProfile()
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
