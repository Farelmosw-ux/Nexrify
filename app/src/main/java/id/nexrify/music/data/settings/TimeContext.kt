package id.nexrify.music.data.settings

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Konteks waktu untuk sapaan di Beranda.
 * Maghrib TIDAK pernah ditebak dari jam tetap: hanya muncul kalau jadwalnya dihitung dari lokasi pengguna.
 */
data class HomeTimeContext(val period: String, val descriptionId: String, val descriptionEn: String)

object TimeContext {
    /** Lama sapaan Maghrib tampil sejak azan, dalam menit. */
    const val MAGHRIB_WINDOW_MINUTES = 20L

    private val MAGHRIB_MIN = LocalTime.of(15, 0)
    private val MAGHRIB_MAX = LocalTime.of(21, 0)

    /**
     * @param maghribTime awal waktu Maghrib hari ini di lokasi pengguna, atau null kalau lokasi/jadwal tidak tersedia.
     *
     * Waktu lain dibuat relatif terhadap Maghrib supaya selalu ada celah dan tidak bertabrakan:
     * Sore berakhir tepat saat Maghrib mulai, lalu Petang baru dimulai setelah jendela Maghrib selesai.
     */
    fun resolve(now: ZonedDateTime, maghribTime: LocalTime? = null): HomeTimeContext {
        val time = now.toLocalTime()
        val maghrib = maghribTime?.takeIf { it >= MAGHRIB_MIN && it <= MAGHRIB_MAX }
        val maghribEnd = maghrib?.plusMinutes(MAGHRIB_WINDOW_MINUTES)

        if (maghrib != null && maghribEnd != null && time >= maghrib && time < maghribEnd) {
            return HomeTimeContext(
                "Maghrib",
                "Waktunya salat Maghrib bagi yang beragama Islam. Yuk, istirahatkan sejenak aktivitasmu dan tunaikan ibadah. 🤍",
                "It's time for Maghrib prayer for those who observe it. Take a short break from your activities and make time for prayer. 🤍"
            )
        }

        val lateAfternoonEnd = maghrib ?: LocalTime.of(17, 15)
        val eveningStart = maghribEnd ?: lateAfternoonEnd
        val eveningEnd = maxOf(LocalTime.of(19, 0), eveningStart.plusMinutes(30))

        val period = when {
            now.hour == 0 && now.minute == 0 -> "Midnight"
            now.hour == 12 && now.minute == 0 -> "Midday"
            now.hour in 0..3 -> "Early Morning"
            time >= LocalTime.of(4, 0) && time < LocalTime.of(5, 30) -> "Before Sunrise"
            time >= LocalTime.of(5, 30) && time < LocalTime.of(10, 0) -> "Morning"
            time >= LocalTime.of(10, 0) && time < LocalTime.of(11, 0) -> "Late Morning"
            time >= LocalTime.of(11, 0) && time < LocalTime.of(15, 0) -> "Afternoon"
            time >= LocalTime.of(15, 0) && time < lateAfternoonEnd -> "Late Afternoon"
            time >= eveningStart && time < eveningEnd -> "Evening"
            time >= eveningEnd && time < LocalTime.of(23, 0) -> "Night"
            else -> "Late Night"
        }
        val id = when (period) {
            "Early Morning" -> "Masih melek? Temani dini harimu dengan musik di Nexrify."
            "Before Sunrise" -> "Menyambut pagi? Mulai harimu dengan musik pilihan di Nexrify."
            "Morning" -> "Awali harimu dengan irama yang bikin semangat bersama Nexrify."
            "Late Morning" -> "Aktivitas mulai padat? Biar musik Nexrify yang nemenin."
            "Afternoon" -> "Lagi istirahat? Putar lagu favoritmu dan nikmati waktumu."
            "Late Afternoon" -> "Santai sejenak, nikmati suasana sore bersama musik di Nexrify."
            "Evening" -> "Langit mulai gelap, waktunya menikmati musik dan melepas penat."
            "Night" -> "Nikmati senja dengan lagu yang cocok untuk suasana hatimu."
            "Late Night" -> "Saatnya bersantai, biarkan musik Nexrify menemani malammu."
            "Midnight" -> "Belum tidur? Temukan irama yang pas untuk menemani malammu."
            else -> "Masih begadang? Temani sunyinya malam dengan musik di Nexrify."
        }
        val en = when (period) {
            "Early Morning" -> "Still awake? Let Nexrify keep you company in the early hours."
            "Before Sunrise" -> "Welcoming the sunrise? Start with music picked for you."
            "Morning" -> "Start your day with a rhythm that lifts your mood."
            "Late Morning" -> "Getting busy? Let Nexrify soundtrack your day."
            "Afternoon" -> "Taking a break? Play your favorite songs and enjoy the moment."
            "Late Afternoon" -> "Take a breather and enjoy the afternoon with Nexrify."
            "Evening" -> "The sky is getting dark. Unwind with music."
            "Night" -> "Find songs that fit your mood tonight."
            "Late Night" -> "Relax and let Nexrify keep you company tonight."
            "Midnight" -> "Still awake? Find the right rhythm for the quiet night."
            else -> "Still up? Let music keep you company in the quiet hours."
        }
        return HomeTimeContext(period, id, en)
    }
}
