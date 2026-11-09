package id.nexrify.music.service

import android.app.Notification
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

/**
 * Notifikasi musik tidak boleh hilang selama lagu diputar, termasuk saat pengguna menekan tombol X (hapus semua).
 * Notifikasi hanya boleh dibuang pengguna setelah musik dijeda atau dimatikan.
 */
@OptIn(UnstableApi::class)
class PersistentNotificationProvider(
    private val delegate: DefaultMediaNotificationProvider
) : MediaNotification.Provider {

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val result = delegate.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
        val player = mediaSession.player
        if (player.isPlaying || player.playWhenReady) {
            result.notification.flags = result.notification.flags or
                Notification.FLAG_ONGOING_EVENT or
                Notification.FLAG_NO_CLEAR
        }
        return result
    }

    override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean =
        delegate.handleCustomCommand(session, action, extras)
}
