package id.nexrify.music.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import id.nexrify.music.MainActivity
import id.nexrify.music.NexrifyApp
import id.nexrify.music.R

/**
 * Widget layar utama: judul, artis, cover, dan tombol prev / play-pause / next.
 * Status terakhir disimpan di SharedPreferences perangkat sehingga widget tetap benar
 * setelah proses aplikasi dimatikan sistem.
 */
class NowPlayingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val views = buildViews(
            context,
            title = prefs.getString(KEY_TITLE, null),
            artist = prefs.getString(KEY_ARTIST, null),
            isPlaying = prefs.getBoolean(KEY_PLAYING, false),
            art = null
        )
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE, ACTION_NEXT, ACTION_PREV -> {
                val app = context.applicationContext as? NexrifyApp ?: return
                val vm = app.musicViewModel
                when (intent.action) {
                    ACTION_TOGGLE -> {
                        // Jika belum ada lagu aktif (proses baru), lanjutkan sesi terakhir.
                        if (vm.currentTrack.value == null) vm.resumeLastPlayback() else vm.togglePlayPause()
                    }
                    ACTION_NEXT -> vm.skipNext()
                    ACTION_PREV -> vm.skipPrevious()
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "id.nexrify.music.widget.TOGGLE"
        const val ACTION_NEXT = "id.nexrify.music.widget.NEXT"
        const val ACTION_PREV = "id.nexrify.music.widget.PREV"

        private const val PREF_NAME = "nexrify_widget"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_PLAYING = "playing"

        /** Dipanggil ViewModel setiap lagu / status putar berubah. */
        fun publish(context: Context, title: String?, artist: String?, isPlaying: Boolean, art: Bitmap?) {
            val app = context.applicationContext
            app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_TITLE, title)
                .putString(KEY_ARTIST, artist)
                .putBoolean(KEY_PLAYING, isPlaying)
                .apply()

            try {
                val manager = AppWidgetManager.getInstance(app)
                val ids = manager.getAppWidgetIds(ComponentName(app, NowPlayingWidgetProvider::class.java))
                if (ids.isEmpty()) return
                manager.updateAppWidget(ids, buildViews(app, title, artist, isPlaying, art))
            } catch (_: Exception) {
                // Widget tidak boleh mengganggu pemutaran.
            }
        }

        private fun buildViews(
            context: Context,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
            art: Bitmap?
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_now_playing)

            if (!title.isNullOrBlank()) {
                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_artist, artist.orEmpty())
            } else {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_idle_title))
                views.setTextViewText(R.id.widget_artist, context.getString(R.string.widget_idle_subtitle))
            }

            views.setImageViewResource(
                R.id.widget_play,
                if (isPlaying) R.drawable.ic_nexrify_pause else R.drawable.ic_nexrify_play
            )
            if (art != null) views.setImageViewBitmap(R.id.widget_art, art)

            views.setOnClickPendingIntent(R.id.widget_play, actionIntent(context, ACTION_TOGGLE, 1))
            views.setOnClickPendingIntent(R.id.widget_next, actionIntent(context, ACTION_NEXT, 2))
            views.setOnClickPendingIntent(R.id.widget_prev, actionIntent(context, ACTION_PREV, 3))

            val open = PendingIntent.getActivity(
                context,
                4,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_art, open)
            views.setOnClickPendingIntent(R.id.widget_title, open)
            views.setOnClickPendingIntent(R.id.widget_artist, open)
            return views
        }

        private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, NowPlayingWidgetProvider::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
