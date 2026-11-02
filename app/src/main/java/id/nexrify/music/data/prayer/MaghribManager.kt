package id.nexrify.music.data.prayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Waktu Maghrib berdasarkan lokasi pengguna, bukan jam tetap.
 *
 * Lokasi diambil dari lokasi kasar perangkat (tanpa Google Play Services) lalu waktu terbenam matahari
 * dihitung langsung di HP, jadi tidak butuh internet. Zona waktu mengikuti zona waktu perangkat, jadi WIB, WITA,
 * dan WIT otomatis benar. Setiap lokasi berubah, jadwal ikut dihitung ulang.
 *
 * Kalau izin lokasi tidak ada atau lokasi tidak bisa didapat, [maghribFor] mengembalikan null.
 * Tidak ada waktu tebakan sama sekali.
 */
class MaghribManager private constructor(private val context: Context) {

    enum class Status { NO_PERMISSION, LOCATING, READY, UNAVAILABLE }

    data class Place(val latitude: Double, val longitude: Double, val timeMs: Long)

    private val prefs = context.getSharedPreferences("nexrify_prayer", Context.MODE_PRIVATE)

    private val _place = MutableStateFlow(loadPlace())
    val place: StateFlow<Place?> = _place.asStateFlow()

    private val _status = MutableStateFlow(Status.LOCATING)
    val status: StateFlow<Status> = _status.asStateFlow()

    @Volatile
    private var requesting = false

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Panggil dari main thread: saat Beranda tampil, tiap 15 menit, dan setelah izin diberikan. */
    fun refresh() {
        if (!hasPermission()) {
            _place.value = null
            prefs.edit().clear().apply()
            _status.value = Status.NO_PERMISSION
            return
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            _status.value = if (usablePlace() != null) Status.READY else Status.UNAVAILABLE
            return
        }

        val fresh = bestLastKnown(lm)
        if (fresh != null && System.currentTimeMillis() - fresh.time <= FRESH_MS) {
            accept(fresh)
            return
        }

        // Pakai lokasi tersimpan sementara menunggu fix baru, selama belum kedaluwarsa.
        _status.value = if (usablePlace() != null) Status.READY else Status.LOCATING
        requestCurrent(lm)
    }

    private fun requestCurrent(lm: LocationManager) {
        if (requesting) return
        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (provider == null) {
            if (usablePlace() == null) _status.value = Status.UNAVAILABLE
            return
        }
        requesting = true
        val mainHandler = Handler(Looper.getMainLooper())
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                finish(lm, this, mainHandler)
                accept(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        try {
            @Suppress("DEPRECATION")
            lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            // Kalau tidak ada fix dalam 30 detik, berhenti menunggu.
            mainHandler.postDelayed({
                if (requesting) {
                    finish(lm, listener, mainHandler)
                    if (usablePlace() == null) _status.value = Status.UNAVAILABLE
                }
            }, 30_000L)
        } catch (_: SecurityException) {
            requesting = false
            _status.value = Status.NO_PERMISSION
        } catch (_: Exception) {
            requesting = false
            if (usablePlace() == null) _status.value = Status.UNAVAILABLE
        }
    }

    private fun finish(lm: LocationManager, listener: LocationListener, handler: Handler) {
        requesting = false
        handler.removeCallbacksAndMessages(null)
        try {
            lm.removeUpdates(listener)
        } catch (_: Exception) {
        }
    }

    private fun bestLastKnown(lm: LocationManager): Location? {
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
        return providers.mapNotNull { p ->
            try {
                lm.getLastKnownLocation(p)
            } catch (_: SecurityException) {
                null
            } catch (_: Exception) {
                null
            }
        }.maxByOrNull { it.time }
    }

    private fun accept(location: Location) {
        val place = Place(location.latitude, location.longitude, System.currentTimeMillis())
        _place.value = place
        prefs.edit()
            .putLong("lat", java.lang.Double.doubleToRawLongBits(place.latitude))
            .putLong("lng", java.lang.Double.doubleToRawLongBits(place.longitude))
            .putLong("time", place.timeMs)
            .apply()
        _status.value = Status.READY
    }

    private fun loadPlace(): Place? {
        if (!prefs.contains("lat")) return null
        val time = prefs.getLong("time", 0L)
        if (System.currentTimeMillis() - time > MAX_AGE_MS) return null
        return Place(
            java.lang.Double.longBitsToDouble(prefs.getLong("lat", 0L)),
            java.lang.Double.longBitsToDouble(prefs.getLong("lng", 0L)),
            time
        )
    }

    private fun usablePlace(): Place? =
        _place.value?.takeIf { System.currentTimeMillis() - it.timeMs <= MAX_AGE_MS }

    /** Awal waktu Maghrib hari [date] di lokasi terakhir, dalam zona [zone]. Null kalau lokasi tidak tersedia. */
    fun maghribFor(date: LocalDate, zone: ZoneId): LocalTime? {
        if (!hasPermission()) return null
        val place = usablePlace() ?: return null
        val sunset = sunset(date, place.latitude, place.longitude, zone) ?: return null
        // Dibulatkan ke menit terdekat supaya sama dengan yang tampil di jam dan adzan.
        return sunset.plusMinutes(IHTIYAT_MINUTES).plusSeconds(30)
            .truncatedTo(java.time.temporal.ChronoUnit.MINUTES).toLocalTime()
    }

    companion object {
        private const val FRESH_MS = 30 * 60 * 1000L
        private const val MAX_AGE_MS = 3 * 24 * 60 * 60 * 1000L
        /** Maghrib = terbenam matahari. Isi 2 kalau ingin mengikuti ihtiyat Kemenag. */
        private const val IHTIYAT_MINUTES = 0L

        @Volatile
        private var instance: MaghribManager? = null

        fun init(context: Context): MaghribManager =
            instance ?: synchronized(this) {
                instance ?: MaghribManager(context.applicationContext).also { instance = it }
            }

        fun getInstance(): MaghribManager =
            instance ?: throw IllegalStateException("MaghribManager belum di inisialisasi")

        private fun rad(d: Double) = d * Math.PI / 180.0
        private fun deg(r: Double) = r * 180.0 / Math.PI
        private fun norm360(v: Double): Double {
            var r = v % 360.0
            if (r < 0) r += 360.0
            return r
        }

        /**
         * Terbenam matahari (pusat matahari di -0,833 derajat) memakai algoritma posisi matahari NOAA,
         * dua putaran supaya selisihnya cuma hitungan detik. Hasil dibuat per tanggal lokal [date].
         */
        internal fun sunset(date: LocalDate, lat: Double, lng: Double, zone: ZoneId): ZonedDateTime? {
            for (shift in longArrayOf(0, -1, 1)) {
                val calcDate = date.plusDays(shift)
                val minutes = sunsetUtcMinutes(calcDate, lat, lng) ?: return null
                val zoned = calcDate.atStartOfDay(ZoneOffset.UTC)
                    .plusSeconds((minutes * 60.0).toLong())
                    .withZoneSameInstant(zone)
                if (zoned.toLocalDate() == date) return zoned
            }
            return null
        }

        /** Menit sejak 00:00 UTC pada tanggal [date] saat matahari terbenam di ([lat], [lng]). */
        private fun sunsetUtcMinutes(date: LocalDate, lat: Double, lng: Double): Double? {
            val jd0 = date.toEpochDay() + 2440587.5
            var minutes = 720.0 - 4.0 * lng
            for (pass in 0 until 2) {
                val jd = jd0 + minutes / 1440.0
                val t = (jd - 2451545.0) / 36525.0
                val l0 = norm360(280.46646 + t * (36000.76983 + t * 0.0003032))
                val m = 357.52911 + t * (35999.05029 - 0.0001537 * t)
                val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
                val c = sin(rad(m)) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
                    sin(rad(2 * m)) * (0.019993 - 0.000101 * t) +
                    sin(rad(3 * m)) * 0.000289
                val omega = 125.04 - 1934.136 * t
                val lambda = l0 + c - 0.00569 - 0.00478 * sin(rad(omega))
                val eps0 = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
                val eps = eps0 + 0.00256 * cos(rad(omega))
                val dec = asin(sin(rad(eps)) * sin(rad(lambda)))
                val tanHalf = tan(rad(eps / 2.0))
                val y = tanHalf * tanHalf
                val eq = 4.0 * deg(
                    y * sin(2 * rad(l0)) -
                        2 * e * sin(rad(m)) +
                        4 * e * y * sin(rad(m)) * cos(2 * rad(l0)) -
                        0.5 * y * y * sin(4 * rad(l0)) -
                        1.25 * e * e * sin(2 * rad(m))
                )
                val cosH = cos(rad(90.833)) / (cos(rad(lat)) * cos(dec)) - tan(rad(lat)) * tan(dec)
                if (cosH > 1.0 || cosH < -1.0) return null
                val ha = deg(acos(cosH))
                minutes = 720.0 - 4.0 * lng - eq + 4.0 * ha
            }
            return minutes
        }
    }
}
