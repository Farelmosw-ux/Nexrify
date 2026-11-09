package id.nexrify.music.service

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Jembatan satu proses antara MusicViewModel dan layanan pemutar (notifikasi dan kapsul).
 * ViewModel mengisi status suka lagu aktif, layanan memakainya untuk ikon hati dan memicu aksi suka.
 */
object PlaybackBridge {
    /** True kalau lagu yang sedang diputar ada di favorit. */
    val isFavorite = MutableStateFlow(false)

    /** True saat ViewModel sedang menyiapkan lagu (ganti lagu). Kapsul tidak boleh menghilang di saat ini. */
    val isLoading = MutableStateFlow(false)

    /** Diisi ViewModel. Dipanggil dari notifikasi atau kapsul untuk menyuka atau batal suka lagu aktif. */
    @Volatile
    var onToggleFavorite: (() -> Unit)? = null
}
