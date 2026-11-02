package id.nexrify.music.data.prayer

import android.content.Context
import id.nexrify.music.data.settings.TimeContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Status popup pengingat Maghrib.
 *
 * Popup muncul sekali per hari, mulai [TimeContext.PRE_MAGHRIB_MINUTES] menit sebelum Maghrib sampai
 * [TimeContext.MAGHRIB_WINDOW_MINUTES] menit sesudahnya, dan hanya kalau jadwal Maghrib dihitung dari lokasi
 * pengguna (izin lokasi aktif). Tanpa lokasi tidak ada popup, tidak ada jam tebakan.
 */
class MaghribReminder private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("nexrify_prayer_reminder", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    /** Jam Maghrib yang ditampilkan di popup. */
    @Volatile
    var shownMaghrib: LocalTime? = null
        private set

    /** Apakah musik sedang diputar saat popup muncul, supaya bisa dilanjutkan saat ditutup. */
    @Volatile
    var musicWasPlaying: Boolean = false

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        if (!value) _visible.value = false
    }

    /** True kalau sekarang saatnya popup muncul dan belum pernah dikonfirmasi hari ini. */
    fun shouldShow(now: ZonedDateTime, maghrib: LocalTime?): Boolean {
        if (!_enabled.value || _visible.value || maghrib == null) return false
        if (acknowledgedDay() == now.toLocalDate().toEpochDay()) return false
        val time = now.toLocalTime()
        val start = maghrib.minusMinutes(TimeContext.PRE_MAGHRIB_MINUTES)
        val end = maghrib.plusMinutes(TimeContext.MAGHRIB_WINDOW_MINUTES)
        return time >= start && time < end
    }

    fun show(maghrib: LocalTime?, wasPlaying: Boolean) {
        shownMaghrib = maghrib
        musicWasPlaying = wasPlaying
        _visible.value = true
    }

    /** Dipanggil setelah pengguna menekan TUTUP atau KELUAR. Tidak muncul lagi sampai besok. */
    fun acknowledge(today: LocalDate = LocalDate.now()) {
        prefs.edit().putLong(KEY_ACK_DAY, today.toEpochDay()).apply()
        _visible.value = false
    }

    private fun acknowledgedDay(): Long = prefs.getLong(KEY_ACK_DAY, -1L)

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_ACK_DAY = "ack_day"

        @Volatile
        private var instance: MaghribReminder? = null

        fun init(context: Context): MaghribReminder =
            instance ?: synchronized(this) {
                instance ?: MaghribReminder(context.applicationContext).also { instance = it }
            }

        fun getInstance(): MaghribReminder =
            instance ?: throw IllegalStateException("MaghribReminder belum di inisialisasi")
    }
}
