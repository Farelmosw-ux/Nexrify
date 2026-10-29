package id.nexrify.music.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import id.nexrify.music.data.settings.AudioQuality
import id.nexrify.music.data.settings.SettingsManager

/**
 * Semua teks yang tampil di aplikasi ada di sini, satu fungsi satu teks, dua bahasa berdampingan.
 * MainActivity menyediakan instance ini lewat [LocalStrings] dan mengamati pilihan bahasa,
 * jadi begitu bahasa diganti semua layar langsung ikut berubah tanpa restart.
 */
@Immutable
class Strings(val english: Boolean) {
    private fun t(id: String, en: String) = if (english) en else id

    // Navigasi
    val navHome get() = t("Beranda", "Home")
    val navSearch get() = t("Cari", "Search")
    val navLibrary get() = t("Koleksi", "Library")

    // Umum
    val close get() = t("Tutup", "Close")
    val cancel get() = t("Batal", "Cancel")
    val create get() = t("Buat", "Create")
    val refresh get() = t("Muat ulang", "Refresh")
    val unknownTrack get() = t("Lagu tanpa judul", "Unknown track")
    val unknownArtist get() = t("Artis tidak dikenal", "Unknown artist")
    val noSongPlaying get() = t("Belum ada lagu diputar", "Nothing playing yet")

    val playbackChannelDesc get() = t("Kontrol pemutaran musik Nexrify", "Nexrify music playback controls")

    // Loading
    val loading get() = t("Memuat", "Loading")
    val loadingSub get() = t("Menyiapkan musikmu", "Getting your music ready")

    // Login YouTube
    val loginYoutube get() = t("Masuk ke YouTube", "Sign in to YouTube")
    val loginYoutubeSub get() = t("Lewat halaman login resmi Google", "Through the official Google sign-in page")

    // Lirik
    val lyrics get() = t("Lirik", "Lyrics")
    val karaokeSync get() = t("LIRIK SINKRON", "SYNCED LYRICS")
    val lyricsPlain get() = t("LIRIK", "LYRICS")
    val translateOn get() = t("文A ID: NYALA", "文A ID: ON")
    val translateOff get() = t("文A ID: MATI", "文A ID: OFF")
    val loadingLyrics get() = t("Lagi nyari lirik…", "Looking for lyrics…")
    val noLyrics get() = t("Lirik sinkron belum tersedia", "No synced lyrics available")

    // Pengaturan
    val settings get() = t("Pengaturan", "Settings")
    val languageTitle get() = t("Bahasa", "Language")
    val langSystem get() = t("Ikuti perangkat", "Device default")
    val langId get() = "Indonesia"
    val langEn get() = "English"
    val updateTitle get() = t("Pembaruan Nexrify", "Nexrify updates")
    val updateDesc get() = t("Cek versi terbaru dari GitHub Releases.", "Check the latest version on GitHub Releases.")
    val updateNotChecked get() = t("Belum dicek", "Not checked yet")
    val updateChecking get() = t("Lagi ngecek pembaruan…", "Checking for updates…")
    val updateNoRelease get() = t("Versi terbaru belum dirilis.", "No release has been published yet.")
    fun updateLatest(current: String) = t("Nexrify kamu sudah yang terbaru ($current).", "Nexrify is up to date ($current).")
    fun updateAvailable(current: String, latest: String) = t("Ada versi baru: $current ke $latest", "Update available: $current to $latest")
    val updateFailed get() = t("Gagal ngecek. Coba cek koneksi atau buka halaman Releases.", "Couldn't check. Try your connection or open the Releases page.")
    val checkUpdate get() = t("Cek pembaruan", "Check for updates")
    val checking get() = t("Lagi ngecek…", "Checking…")
    val openReleases get() = t("Buka Releases", "Open Releases")
    val cannotOpenUpdate get() = t("Halaman pembaruan nggak bisa dibuka", "Couldn't open the update page")

    val audioQualityTitle get() = t("Kualitas audio streaming", "Streaming audio quality")
    fun qualityTitle(q: AudioQuality) = when (q) {
        AudioQuality.HIGH -> t("Kualitas tinggi (M4A / 256 kbps)", "High quality (M4A / 256 kbps)")
        AudioQuality.MEDIUM -> t("Kualitas normal (128 kbps)", "Normal quality (128 kbps)")
        AudioQuality.LOW -> t("Hemat data (64 kbps)", "Data saver (64 kbps)")
    }
    fun qualityDesc(q: AudioQuality) = when (q) {
        AudioQuality.HIGH -> t("Suara paling jernih", "Crisp sound, best fidelity")
        AudioQuality.MEDIUM -> t("Seimbang antara kecepatan dan kuota", "Balanced speed and data usage")
        AudioQuality.LOW -> t("Buffer paling cepat, kuota paling irit", "Fastest buffering, lowest data use")
    }
    val selected get() = t("Dipilih", "Selected")

    val themeTitle get() = t("Tema tampilan", "Appearance")
    val themeSystem get() = t("Ikuti sistem", "Follow system")
    val themeSystemDesc get() = t("Sesuai mode tema di HP kamu", "Matches your phone's theme")
    val themeDark get() = t("Mode gelap", "Dark mode")
    val themeDarkDesc get() = t("Hitam OLED, lebih hemat baterai", "OLED black, easier on the battery")
    val themeLight get() = t("Mode terang", "Light mode")
    val themeLightDesc get() = t("Putih bersih dan simpel", "Clean and simple white")

    val lyricsTranslateTitle get() = t("Terjemahan lirik ke Indonesia", "Lyrics translation to Indonesian")
    val lyricsTranslateDesc get() = t("Tampilkan arti lirik dalam bahasa Indonesia", "Show the meaning of lyrics in Indonesian")

    val storageTitle get() = t("Penyimpanan dan cache", "Storage and cache")
    fun cacheSize(size: String) = t("Ukuran cache: $size", "Cache size: $size")
    val calculating get() = t("Lagi ngitung…", "Calculating…")
    val cacheCleared get() = t("Cache sudah dibersihkan", "Cache cleared")
    val clear get() = t("Bersihkan", "Clear")

    // Panel kapsul
    val capsuleTitle get() = t("Panel musik kapsul", "Music capsule panel")
    val capsuleDesc get() = t(
        "Kapsul kecil di atas layar saat lagu diputar dan kamu lagi di luar Nexrify. Tekan sekali untuk buka aplikasi, tekan lama untuk kontrol penuh.",
        "A small capsule at the top of the screen while music plays and you're outside Nexrify. Tap to open the app, long press for full controls."
    )
    val capsulePermission get() = t("Butuh izin tampil di atas aplikasi lain", "Needs permission to display over other apps")
    val capsuleGrant get() = t("Kasih izin", "Grant permission")
    val capsulePermissionHint get() = t("Izin ini cuma dipakai buat nampilin kapsul.", "This permission is only used to show the capsule.")
    val capsuleNeedPermissionToast get() = t("Aktifkan izin dulu ya, terus balik lagi ke sini", "Grant the permission first, then come back here")

    // Footer
    val footerTagline get() = t("Musik tanpa iklan, ringan, dan enak didengar.", "Ad free music that stays light and sounds good.")
    val footerMadeBy get() = t("Dibuat oleh Nexra Destroyer", "Made by Nexra Destroyer")
    fun footerVersion(version: String) = t("Versi $version", "Version $version")
    val footerPrivate get() = t(
        "Kode sumber Nexrify bersifat privat. Aplikasi hanya bisa diunduh lewat tautan resmi dari pengembang.",
        "Nexrify's source code is private. The app is only available through the developer's official links."
    )

    // Profil
    val musicLover get() = t("Pecinta Musik Nexrify", "Nexrify Music Lover")
    val googleConnected get() = t("Akun Google tersambung", "Google account connected")
    val tapToLogin get() = t("Ketuk buat masuk dengan Google", "Tap to sign in with Google")
    val switchAccount get() = t("Ganti atau keluar akun", "Switch or sign out")
    val connectGoogle get() = t("Hubungkan akun Google", "Connect Google account")
    val statsTitle get() = t("Statistik mendengarkan", "Listening stats")
    val statsSub get() = t("Selera dan artis favoritmu", "Your taste and favorite artists")
    val recentTitle get() = t("Terakhir diputar", "Recently played")
    val recentSubEmpty get() = t("Belum ada riwayat pemutaran", "No playback history yet")
    fun recentSubCount(n: Int) = t("$n lagu di riwayat", "$n songs in history")
    val settingsPrivacy get() = t("Pengaturan dan privasi", "Settings and privacy")
    val settingsPrivacySub get() = t("Kualitas audio, tema, cache, dan preferensi", "Audio quality, theme, cache and preferences")
    val noRecent get() = t("Belum ada lagu yang diputar.", "No songs played yet.")
    val totalPlays get() = t("Total putar", "Total plays")
    val artistsHeard get() = t("Artis didengar", "Artists heard")
    val notEnoughData get() = t("Datanya belum cukup. Putar lebih banyak lagu biar diagramnya muncul.", "Not enough data yet. Play more songs to see the chart.")
    val artistChartTitle get() = t("DIAGRAM FREKUENSI ARTIS", "ARTIST FREQUENCY CHART")
    fun topN(n: Int) = t("Top $n", "Top $n")
    val topArtistsRanking get() = t("Peringkat artis teratas", "Top artists ranking")

    // Playlist
    val newPlaylist get() = t("Buat playlist baru", "Create new playlist")
    val newPlaylistHint get() = t("Kumpulkan lagu sesuai suasana hatimu", "Collect songs that match your mood")
    val choosePlaylist get() = t("Pilih playlist", "Choose a playlist")
    val noPlaylistCreateFirst get() = t("Belum ada playlist. Bikin yang pertama yuk!", "No playlists yet. Make your first one!")
    val namePlaylist get() = t("Kasih nama playlist", "Name your playlist")
    val playlistNameHint get() = t("Nama playlist…", "Playlist name…")
    val playlistNameExample get() = t("Contoh: Galau Malam, Workout…", "Example: Late Night, Workout…")
    val createAndAdd get() = t("Buat dan tambahkan", "Create and add")
    val noPlaylistTitle get() = t("Belum ada playlist", "No playlists yet")
    val noPlaylistHint get() = t("Tekan tombol di atas buat bikin playlist", "Press the button above to create one")
    val playAll get() = t("Putar semua", "Play all")
    val shuffle get() = t("Acak", "Shuffle")
    val playlistEmpty get() = t("Playlist ini masih kosong. Tambahin lagu dari pencarian atau beranda ya.", "This playlist is empty. Add songs from search or the home page.")

    // Pemutar
    val nowPlaying get() = t("SEDANG DIPUTAR", "NOW PLAYING")
    val addToPlaylist get() = t("Tambah ke playlist", "Add to playlist")
    val settingsAudio get() = t("Pengaturan dan audio", "Settings and audio")
    val shareSong get() = t("Bagikan lagu", "Share song")
    val shareVia get() = t("Bagikan lewat", "Share via")
    fun shareText(title: String, artist: String, url: String) = t("Dengerin $title dari $artist: $url", "Listen to $title by $artist: $url")

    // Koleksi
    val library get() = t("Koleksi", "Library")
    fun likedCount(n: Int) = t("$n lagu disukai", "$n liked songs")
    fun playlistCount(n: Int) = t("$n playlist", "$n playlists")
    val likedSongs get() = t("Lagu disukai", "Liked songs")
    fun playlistsTab(n: Int) = t("Playlist ($n)", "Playlists ($n)")
    val libraryEmpty get() = t("Koleksimu masih kosong", "Your library is empty")
    val libraryEmptyHint get() = t("Ketuk ikon hati di lagu mana pun buat nyimpen di sini", "Tap the heart on any song to save it here")

    // Beranda
    val genreAll get() = t("Semua", "All")
    fun genreLabel(key: String) = if (key == "Semua") genreAll else key
    fun noSongsFor(genre: String) = t("Nggak ada lagu buat $genre", "No songs found for $genre")
    val recentlyPlayed get() = t("Baru diputar", "Recently played")
    val favoriteSongs get() = t("Lagu favorit", "Favorite songs")
    fun sectionTitle(raw: String) = when (raw) {
        "Lagi Sering Kamu Putar" -> t(raw, "On repeat for you")
        "Pilihan Cepat" -> t(raw, "Quick picks")
        "Lagu Trending" -> t(raw, "Trending songs")
        else -> raw
    }
    fun sectionSubtitle(raw: String) = when (raw) {
        "Berdasarkan selera musikmu" -> t(raw, "Based on your taste")
        "Dengarkan musik favoritmu" -> t(raw, "Listen to your favorite music")
        "Paling banyak diputar" -> t(raw, "Most played right now")
        else -> raw
    }
    fun period(raw: String) = when (raw) {
        "Maghrib" -> t("Maghrib", "Maghrib")
        "Midnight" -> t("Tengah malam", "Midnight")
        "Midday" -> t("Tengah hari", "Midday")
        "Early Morning" -> t("Dini hari", "Early morning")
        "Before Sunrise" -> t("Menjelang pagi", "Before sunrise")
        "Morning" -> t("Selamat pagi", "Good morning")
        "Late Morning" -> t("Menjelang siang", "Late morning")
        "Afternoon" -> t("Selamat siang", "Good afternoon")
        "Late Afternoon" -> t("Selamat sore", "Late afternoon")
        "Evening" -> t("Petang", "Evening")
        "Night" -> t("Selamat malam", "Good evening")
        "Late Night" -> t("Larut malam", "Late night")
        else -> raw
    }

    // Cari
    val searchTitle get() = t("Cari", "Search")
    val searchHint get() = t("Lagu, artis, genre…", "Songs, artists, genres…")
    val exploreMusic get() = t("Jelajahi musik", "Explore music")
    val noTracks get() = t("Lagunya nggak ketemu", "No tracks found")
    val searchHistory get() = t("Riwayat pencarian", "Search history")
    val clearAll get() = t("Hapus semua", "Clear all")
    val noHistory get() = t("Belum ada riwayat pencarian", "No search history yet")
    val noHistoryHint get() = t("Hal yang kamu cari bakal muncul di sini", "Things you search for will show up here")
    val deleteHistoryItem get() = t("Hapus dari riwayat", "Remove from history")
    val clearHistoryTitle get() = t("Hapus semua riwayat?", "Clear all history?")
    val clearHistoryBody get() = t("Semua pencarian yang tersimpan akan dihapus.", "All saved searches will be removed.")
    val delete get() = t("Hapus", "Delete")
    val back get() = t("Kembali", "Back")
    val tapToSearch get() = t("Ketuk buat nyari lagu, artis, atau genre", "Tap to look for songs, artists or genres")
    fun categoryLabel(key: String) = when (key) {
        "Hits Global" -> t("Hits Global", "Global Hits")
        "Lo-Fi & Santai" -> t("Lo-Fi & Santai", "Lo-Fi & Chill")
        "Anime & J-Pop" -> "Anime & J-Pop"
        "Fokus Mendalam" -> t("Fokus Mendalam", "Deep Focus")
        "Hip-Hop & R&B" -> "Hip-Hop & R&B"
        "Akustik & Folk" -> t("Akustik & Folk", "Acoustic & Folk")
        "Rock & Alternatif" -> t("Rock & Alternatif", "Rock & Alternative")
        "Elektronik & EDM" -> t("Elektronik & EDM", "Electronic & EDM")
        else -> key
    }
    fun categoryDesc(key: String) = when (key) {
        "Hits Global" -> t("Lagu populer saat ini", "Popular right now")
        "Lo-Fi & Santai" -> t("Temani waktu rehat", "For your break time")
        "Anime & J-Pop" -> t("Soundtrack dan lagu Jepang", "Soundtracks and Japanese songs")
        "Fokus Mendalam" -> t("Belajar dan coding", "Studying and coding")
        "Hip-Hop & R&B" -> t("Beat dan ritme pilihan", "Hand picked beats and rhythm")
        "Akustik & Folk" -> t("Suara hangat dan natural", "Warm and natural sound")
        "Rock & Alternatif" -> t("Energi dan gitar kuat", "Big energy, loud guitars")
        else -> t("Elektronik dan suasana festival", "Electronic and festival vibes")
    }

    // Maghrib dan lokasi
    val maghribTitle get() = t("Sapaan Maghrib", "Maghrib greeting")
    val maghribDesc get() = t(
        "Dihitung dari lokasi dan zona waktumu, jadi pas dengan kotamu dan ikut berubah kalau kamu pindah tempat.",
        "Calculated from your location and time zone, so it fits your city and follows you when you move."
    )
    val maghribNoPermission get() = t(
        "Izin lokasi belum diberikan, jadi sapaan Maghrib belum tampil. Musik tetap bisa dipakai seperti biasa.",
        "Location permission isn't granted, so the Maghrib greeting isn't shown. Music works as usual."
    )
    val maghribLocating get() = t("Lagi nyari lokasimu…", "Finding your location…")
    val maghribUnavailable get() = t(
        "Lokasi belum bisa didapat, jadi sapaan Maghrib dimatikan dulu. Nyalakan lokasi HP lalu coba lagi.",
        "Couldn't get your location, so the Maghrib greeting is off for now. Turn on location and try again."
    )
    fun maghribReady(time: String) = t("Maghrib hari ini sekitar $time di lokasimu.", "Maghrib today is around $time at your location.")
    val maghribNote get() = t("Hitungan astronomi, bisa selisih 1 sampai 3 menit dari jadwal resmi.", "Astronomical calculation, may differ by 1 to 3 minutes from the official schedule.")
    val maghribGrant get() = t("Izinkan lokasi", "Allow location")
    val maghribRetry get() = t("Coba lagi", "Try again")
    val maghribHomeHint get() = t("Aktifkan lokasi di Pengaturan biar sapaan Maghrib sesuai jadwal di kotamu.", "Turn on location in Settings so the Maghrib greeting follows your city's schedule.")
    val googleUser get() = t("Pengguna Google", "Google user")

    val likeAction get() = t("Suka", "Like")
    val unlikeAction get() = t("Batal suka", "Unlike")

    // Mode offline
    val offlineNotDownloaded get() = t("Lagu ini belum diunduh dan kamu lagi offline", "This song isn't downloaded and you're offline")
    val offlineBanner get() = t("Lagi offline. Lagu yang sudah diunduh tetap bisa diputar.", "You're offline. Downloaded songs still play.")
    val offlineBannerAction get() = t("Buka Offline", "Open Offline")

    // Unduhan offline
    val offlineTab get() = t("Offline", "Offline")
    val offlineTitle get() = t("Unduhan offline", "Offline downloads")
    fun offlineSummary(n: Int, size: String) = t("$n lagu, $size", "$n songs, $size")
    val offlineEmpty get() = t("Belum ada lagu offline", "No offline songs yet")
    val offlineEmptyHint get() = t("Buka menu titik tiga di lagu mana pun lalu pilih Unduh offline", "Open the three dot menu on any song and choose Download offline")
    val offlinePlayAll get() = t("Putar semua", "Play all")
    val offlineShuffle get() = t("Acak", "Shuffle")
    val offlineDeleteAll get() = t("Hapus semua", "Delete all")
    val offlineDeleteAllTitle get() = t("Hapus semua unduhan?", "Delete all downloads?")
    val offlineDeleteAllBody get() = t("Semua lagu offline di akun ini akan dihapus dari aplikasi. Kamu bisa mengunduhnya lagi nanti.", "All offline songs for this account will be removed from the app. You can download them again later.")
    val offlineDeleteSong get() = t("Hapus unduhan", "Remove download")
    val offlineDownloading get() = t("Sedang diunduh", "Downloading")
    val offlineQueued get() = t("Menunggu antrean", "Waiting in line")
    val offlineFailed get() = t("Gagal diunduh", "Download failed")
    val offlineRetry get() = t("Coba lagi", "Retry")
    val offlineCancel get() = t("Batalkan", "Cancel")
    val offlineAccountNote get() = t("Unduhan hanya ada di dalam Nexrify dan terikat ke akun yang sedang masuk. Tidak muncul di galeri atau file manager.", "Downloads live inside Nexrify only and belong to the account you're signed in with. They don't show up in your gallery or file manager.")
    val offlineGuestNote get() = t("Kamu belum masuk, jadi unduhan disimpan untuk mode tamu. Masuk dengan akun Google biar unduhan terikat ke akunmu.", "You're not signed in, so downloads are kept for guest mode. Sign in with Google to tie them to your account.")
    val downloadTile get() = t("Unduh offline", "Download offline")
    val downloadedTile get() = t("Hapus unduhan", "Remove download")
    fun downloadingTile(pct: Int) = t("Mengunduh $pct%", "Downloading $pct%")
    val downloadQueuedTile get() = t("Menunggu antrean", "Waiting in line")
    val toastDownloadStarted get() = t("Mulai mengunduh", "Download started")
    val toastDownloadRemoved get() = t("Unduhan dihapus", "Download removed")
    val downloadNotifTitle get() = t("Mengunduh lagu", "Downloading songs")
    fun downloadNotifText(count: Int, pct: Int) = t("$count lagu, $pct%", "$count songs, $pct%")
    val downloadChannelName get() = t("Unduhan offline", "Offline downloads")

    // Antrian
    val queueTitle get() = t("Antrian", "Queue")
    fun queueCount(n: Int) = t("$n lagu", "$n songs")
    val queuePlayed get() = t("Sudah diputar", "Played")
    val queueNowPlaying get() = t("Sedang diputar", "Now playing")
    val queueNext get() = t("Berikutnya", "Up next")
    val queueEmpty get() = t("Antrian masih kosong", "Your queue is empty")
    val queueEmptyHint get() = t("Putar lagu atau tambah lewat menu titik tiga di daftar lagu", "Play a song or add one from the three dot menu on any song")
    val clearUpcoming get() = t("Bersihkan berikutnya", "Clear up next")
    val playNextAction get() = t("Putar berikutnya", "Play next")
    val addToQueueAction get() = t("Tambah ke antrian", "Add to queue")
    val toastPlayNext get() = t("Akan diputar setelah lagu ini", "Will play after this song")
    val toastQueued get() = t("Ditambahkan ke antrian", "Added to queue")
    val moveUp get() = t("Naikkan", "Move up")
    val moveDown get() = t("Turunkan", "Move down")
    val removeFromQueue get() = t("Hapus dari antrian", "Remove from queue")

    // Perangkat audio
    fun deviceLabel(raw: String) = when {
        raw == "Phone Speaker" -> t("Speaker HP", "Phone speaker")
        raw == "Wired Connection" -> t("Headset berkabel", "Wired headset")
        else -> raw
    }

    /** Deskripsi aksesibilitas (dibaca TalkBack). Kunci berupa teks Inggris aslinya. */
    fun cd(key: String): String {
        if (english) return key
        return when (key) {
            "New Playlist" -> "Playlist baru"
            "Delete" -> "Hapus"
            "Back" -> "Kembali"
            "Playing" -> "Sedang diputar"
            "Login YouTube" -> "Masuk YouTube"
            "Account Profile" -> "Profil akun"
            "More Options" -> "Opsi lainnya"
            "Favorite" -> "Favorit"
            "Translate" -> "Terjemahkan"
            "Logout" -> "Keluar"
            "Add" -> "Tambah"
            "Next" -> "Berikutnya"
            "Shuffle" -> "Acak"
            "Repeat" -> "Ulangi"
            "Previous Track" -> "Lagu sebelumnya"
            "Next Track" -> "Lagu berikutnya"
            "Options" -> "Opsi"
            "Lyrics" -> "Lirik"
            "Collapse Player" -> "Kecilkan pemutar"
            "Audio Output" -> "Output audio"
            "Add to playlist" -> "Tambah ke playlist"
            "Queue" -> "Antrian"
            "New" -> "Baru"
            "Added" -> "Sudah ditambahkan"
            "Pause" -> "Jeda"
            "Play" -> "Putar"
            "Collapse" -> "Ciutkan"
            "Expand" -> "Lebarkan"
            "Settings & Equalizer" -> "Pengaturan dan equalizer"
            else -> key
        }
    }

}

val LocalStrings = staticCompositionLocalOf { Strings(false) }

/** Pemakaian di dalam composable: `S.settings`. Ikut recompose saat bahasa berganti. */
val S: Strings
    @Composable
    @ReadOnlyComposable
    get() = LocalStrings.current

/** Untuk kode non composable (event handler, ViewModel, service, widget). Selalu baca bahasa terbaru. */
fun currentStrings(): Strings = Strings(SettingsManager.getInstance().isEnglish())
