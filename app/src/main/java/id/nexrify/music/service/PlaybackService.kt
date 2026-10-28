package id.nexrify.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import id.nexrify.music.MainActivity
import id.nexrify.music.NexrifyWidgetProvider
import id.nexrify.music.R
import id.nexrify.music.i18n.currentStrings
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class PlaybackService : MediaSessionService() {

    private var player: Player? = null
    private var mediaSession: MediaSession? = null
    private var capsule: id.nexrify.music.capsule.MusicCapsuleController? = null
    private val artworkExecutor = Executors.newSingleThreadExecutor()
    @Volatile private var latestArtworkUrl: String? = null
    @Volatile private var latestArtworkTitle: String? = null

    companion object {
        const val CHANNEL_ID = "nexrify_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_NEXT = "id.nexrify.music.ACTION_NEXT"
        const val ACTION_PREV = "id.nexrify.music.ACTION_PREV"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // 1. Ensure Notification Channel exists to prevent Android ANR / crash on startForeground
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setMp4ExtractorFlags(androidx.media3.extractor.mp4.Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)
            .setFragmentedMp4ExtractorFlags(androidx.media3.extractor.mp4.FragmentedMp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory, extractorsFactory)

        // Ultra-low latency buffering with robust fMP4/AAC headroom
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("NexrifyPlaybackService", "ExoPlayer Error: ${error.message}", error)
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateHomeWidget(exoPlayer)
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                updateHomeWidget(exoPlayer)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateHomeWidget(exoPlayer)
            }
        })

        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(exoPlayer) {
            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun seekToNext() {
                sendBroadcast(Intent(ACTION_NEXT).setPackage(packageName))
            }

            override fun seekToNextMediaItem() {
                sendBroadcast(Intent(ACTION_NEXT).setPackage(packageName))
            }

            override fun seekToPrevious() {
                sendBroadcast(Intent(ACTION_PREV).setPackage(packageName))
            }

            override fun seekToPreviousMediaItem() {
                sendBroadcast(Intent(ACTION_PREV).setPackage(packageName))
            }
        }

        player = forwardingPlayer

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_notification)

        setMediaNotificationProvider(PersistentNotificationProvider(notificationProvider))

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(pendingIntent)
            .build()

        // Panel musik kapsul di atas layar (butuh izin tampil di atas aplikasi lain, diatur dari Pengaturan).
        capsule = id.nexrify.music.capsule.MusicCapsuleController(this, forwardingPlayer).also { it.start() }
    }

    private fun updateHomeWidget(player: ExoPlayer) {
        val metadata = player.mediaMetadata
        val title = metadata.title?.toString().orEmpty()
        val artist = metadata.artist?.toString().orEmpty()
        val artworkUrl = metadata.artworkUri?.toString().orEmpty()
        NexrifyWidgetProvider.save(this, title, artist, player.isPlaying)
        NexrifyWidgetProvider.refresh(this)

        // RemoteViews cannot load a network image itself. Fetch artwork off the main thread
        // and only apply it if it still belongs to the current track.
        if (artworkUrl.isNotBlank() && (artworkUrl != latestArtworkUrl || title != latestArtworkTitle)) {
            latestArtworkUrl = artworkUrl
            latestArtworkTitle = title
            artworkExecutor.execute {
                var connection: HttpURLConnection? = null
                try {
                    connection = (URL(artworkUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8_000
                        readTimeout = 8_000
                        instanceFollowRedirects = true
                    }
                    val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
                    if (bitmap != null && latestArtworkUrl == artworkUrl) {
                        NexrifyWidgetProvider.updateArtwork(this, bitmap)
                    }
                } catch (e: Exception) {
                    Log.w("NexrifyPlaybackService", "Unable to load widget artwork", e)
                } finally {
                    connection?.disconnect()
                }
            }
        }
    }

    /** Saat musik diputar, notifikasi selalu dipasang sebagai foreground (ongoing) supaya tidak bisa dihapus. */
    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired || session.player.playWhenReady)
    }

    /** Keep the media session alive when the user swipes the app task away during playback. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (player?.isPlaying != true) super.onTaskRemoved(rootIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = currentStrings().playbackChannelDesc
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        capsule?.destroy()
        capsule = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        artworkExecutor.shutdownNow()
        super.onDestroy()
    }
}
