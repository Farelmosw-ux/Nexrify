package id.nexrify.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import id.nexrify.music.MainActivity
import id.nexrify.music.R
import id.nexrify.music.data.download.DownloadState
import id.nexrify.music.data.download.OfflineManager
import id.nexrify.music.i18n.currentStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Menjaga unduhan tetap jalan saat aplikasi di latar belakang dan menampilkan progresnya di notifikasi. */
class OfflineDownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val first = buildNotification(0, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, first)
        }

        if (job?.isActive != true) {
            job = scope.launch {
                OfflineManager.getInstance().downloads.collect { map ->
                    val running = map.values.filter { it.state !is DownloadState.Failed }
                    if (running.isEmpty()) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        val active = running.firstNotNullOfOrNull { it.state as? DownloadState.Downloading }
                        val pct = ((active?.progress ?: 0f) * 100f).toInt()
                        val nm = getSystemService(NotificationManager::class.java)
                        nm.notify(NOTIFICATION_ID, buildNotification(running.size, pct))
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(count: Int, percent: Int): Notification {
        val strings = currentStrings()
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(strings.downloadNotifTitle)
            .setContentText(strings.downloadNotifText(count, percent))
            .setProgress(100, percent, count == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, currentStrings().downloadChannelName, NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "nexrify_downloads"
        const val NOTIFICATION_ID = 4107
    }
}
