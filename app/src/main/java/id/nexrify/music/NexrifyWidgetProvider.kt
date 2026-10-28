package id.nexrify.music


import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.KeyEvent
import android.widget.RemoteViews

/**
 * Widget layar utama. Tombol memakai media key sehingga diteruskan ke MediaSession
 * aktif (sama seperti tombol headset), tanpa perlu membuka aplikasi.
 */
class NexrifyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, buildViews(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PREV -> sendKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            ACTION_PLAY -> sendKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            ACTION_NEXT -> sendKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    companion object {
        private const val PREFS = "nexrify_widget"
        private const val ACTION_PREV = "id.nexrify.music.WIDGET_PREV"
        private const val ACTION_PLAY = "id.nexrify.music.WIDGET_PLAY"
        private const val ACTION_NEXT = "id.nexrify.music.WIDGET_NEXT"
        @Volatile private var cachedArtwork: android.graphics.Bitmap? = null
        @Volatile private var cachedArtworkTitle: String? = null

        fun save(context: Context, title: String, artist: String, playing: Boolean) {
            if (title != cachedArtworkTitle) {
                cachedArtwork = null
                cachedArtworkTitle = title
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("title", title)
                .putString("artist", artist)
                .putBoolean("playing", playing)
                .apply()
        }

        fun updateArtwork(context: Context, bitmap: android.graphics.Bitmap) {
            cachedArtwork = bitmap
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NexrifyWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = buildViews(context)
            views.setImageViewBitmap(R.id.widget_artwork, bitmap)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NexrifyWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = buildViews(context)
            for (id in ids) manager.updateAppWidget(id, views)
        }

        private fun buildViews(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val title = prefs.getString("title", "").orEmpty()
            val artist = prefs.getString("artist", "").orEmpty()
            val playing = prefs.getBoolean("playing", false)

            val views = RemoteViews(context.packageName, R.layout.widget_player)
            val artwork = cachedArtwork
            if (artwork != null) views.setImageViewBitmap(R.id.widget_artwork, artwork)
            else views.setImageViewResource(R.id.widget_artwork, R.mipmap.ic_launcher)
            views.setTextViewText(R.id.widget_title, if (title.isBlank()) "Nexrify" else title)
            views.setTextViewText(
                R.id.widget_artist,
                if (title.isBlank()) id.nexrify.music.i18n.currentStrings().noSongPlaying else artist
            )
            views.setImageViewResource(
                R.id.widget_play,
                if (playing) R.drawable.ic_nexrify_pause else R.drawable.ic_nexrify_play
            )
            views.setOnClickPendingIntent(R.id.widget_prev, actionIntent(context, ACTION_PREV, 1))
            views.setOnClickPendingIntent(R.id.widget_play, actionIntent(context, ACTION_PLAY, 2))
            views.setOnClickPendingIntent(R.id.widget_next, actionIntent(context, ACTION_NEXT, 3))

            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launch != null) {
                val open = PendingIntent.getActivity(
                    context, 4, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.widget_root, open)
            }
            return views
        }

        private fun actionIntent(context: Context, action: String, code: Int): PendingIntent {
            val intent = Intent(context, NexrifyWidgetProvider::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun sendKey(context: Context, keyCode: Int) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }
}
