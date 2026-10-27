package id.nexrify.music.data.settings

import java.time.LocalTime
import java.time.ZonedDateTime

/** Local-time context. Magrib uses a timezone-based estimate only (WIB/WITA/WIT), not a location-specific prayer timetable. */
data class HomeTimeContext(val period: String, val descriptionId: String, val descriptionEn: String)

object TimeContext {
    private data class MagribSchedule(val timezone: String, val time: LocalTime)

    /**
     * Uses the device's current UTC offset to select a WIB/WITA/WIT estimate.
     * These are approximate display windows, not an authoritative prayer schedule;
     * actual Magrib time varies by city and date.
     */
    private fun timezoneMagrib(now: ZonedDateTime): MagribSchedule? {
        val offsetHours = now.offset.totalSeconds / 3600
        return when (offsetHours) {
            7 -> MagribSchedule("WIB", LocalTime.of(18, 0))
            8 -> MagribSchedule("WITA", LocalTime.of(18, 10))
            9 -> MagribSchedule("WIT", LocalTime.of(18, 20))
            else -> null
        }
    }

    fun resolve(now: ZonedDateTime, maghribTime: LocalTime? = null): HomeTimeContext {
        val time = now.toLocalTime()
        val schedule = if (maghribTime == null) timezoneMagrib(now) else MagribSchedule("local", maghribTime)
        if (schedule != null) {
            val elapsed = java.time.Duration.between(schedule.time, time).toMinutes()
            if (elapsed in 0..20) return HomeTimeContext(
                "Magrib",
                "Waktunya salat Magrib bagi yang beragama Islam. Yuk, istirahatkan sejenak aktivitasmu dan tunaikan ibadah. 🤍",
                "It's time for Maghrib prayer for Muslims. Take a short break and make time for prayer. 🤍"
            )
        }

        val period = when {
            now.hour == 0 && now.minute == 0 -> "Midnight"
            now.hour == 12 && now.minute == 0 -> "Midday"
            now.hour in 0..3 -> "Early Morning"
            time >= LocalTime.of(4, 0) && time < LocalTime.of(5, 30) -> "Before Sunrise"
            time >= LocalTime.of(5, 30) && time < LocalTime.of(10, 0) -> "Morning"
            time >= LocalTime.of(10, 0) && time < LocalTime.of(11, 0) -> "Late Morning"
            time >= LocalTime.of(11, 0) && time < LocalTime.of(15, 0) -> "Afternoon"
            time >= LocalTime.of(15, 0) && time < LocalTime.of(17, 30) -> "Late Afternoon"
            time >= LocalTime.of(17, 30) && time < LocalTime.of(19, 0) -> "Evening"
            time >= LocalTime.of(19, 0) && time < LocalTime.of(23, 0) -> "Night"
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
