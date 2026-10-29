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
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
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
        return sunset.plusMinutes(IHTIYAT_MINUTES).toLocalTime()
    }

    companion object {
        private const val FRESH_MS = 30 * 60 * 1000L
        private const val MAX_AGE_MS = 3 * 24 * 60 * 60 * 1000L
        private const val IHTIYAT_MINUTES = 2L

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

        /** Terbenam matahari (pusat matahari di -0,833 derajat), algoritma almanak astronomi. Akurat sekitar 1 sampai 2 menit. */
        internal fun sunset(date: LocalDate, lat: Double, lng: Double, zone: ZoneId): ZonedDateTime? {
            val n = date.dayOfYear
            val lngHour = lng / 15.0
            val t = n + ((18.0 - lngHour) / 24.0)
            val m = 0.9856 * t - 3.289
            val l = norm360(m + 1.916 * sin(rad(m)) + 0.020 * sin(rad(2 * m)) + 282.634)
            var ra = norm360(deg(atan(0.91764 * tan(rad(l)))))
            ra = (ra + (floor(l / 90.0) * 90.0 - floor(ra / 90.0) * 90.0)) / 15.0
            val sinDec = 0.39782 * sin(rad(l))
            val cosDec = cos(asin(sinDec))
            val cosH = (cos(rad(90.833)) - sinDec * sin(rad(lat))) / (cosDec * cos(rad(lat)))
            if (cosH > 1.0 || cosH < -1.0) return null
            val h = deg(acos(cosH)) / 15.0
            val localMeanTime = h + ra - 0.06571 * t - 6.622
            var ut = (localMeanTime - lngHour) % 24.0
            if (ut < 0) ut += 24.0
            for (shift in longArrayOf(0, -1, 1)) {
                val base = date.plusDays(shift).atStartOfDay(ZoneOffset.UTC)
                val zoned = base.plusSeconds((ut * 3600.0).toLong()).withZoneSameInstant(zone)
                if (zoned.toLocalDate() == date) return zoned
            }
            return null
        }
    }
}
