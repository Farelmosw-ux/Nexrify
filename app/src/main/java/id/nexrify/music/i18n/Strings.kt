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
    val updateDesc get() = t("Cek versi terbaru dari nexrify.biz.id.", "Check the latest version from nexrify.biz.id.")
    val updateNotChecked get() = t("Belum dicek", "Not checked yet")
    val updateChecking get() = t("Lagi ngecek pembaruan…", "Checking for updates…")
    val updateNoRelease get() = t("Versi terbaru belum dirilis.", "No release has been published yet.")
    fun updateLatest(current: String) = t("Nexrify kamu sudah yang terbaru ($current).", "Nexrify is up to date ($current).")
    fun updateAvailable(current: String, latest: String) = t("Ada versi baru: $current ke $latest", "Update available: $current to $latest")
    val updateFailed get() = t("Gagal ngecek. Coba cek koneksimu atau buka website.", "Couldn't check. Try your connection or open the website.")
    val checkUpdate get() = t("Cek pembaruan", "Check for updates")
    val checking get() = t("Lagi ngecek…", "Checking…")
    val openReleases get() = t("Buka website", "Open website")
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
    val capsuleLockedTap get() = t("Aplikasi terkunci dan tidak bisa dibuka lewat kapsul. Buka kunci dulu untuk membuka aplikasi.", "The app is locked and can't be opened from the capsule. Unlock it first to open the app.")
    val capsuleLockedOn get() = t("Terkunci. Ketuk kapsul tidak akan membuka aplikasi.", "Locked. Tapping the capsule won't open the app.")
    val capsuleLockedOff get() = t("Kunci dibuka. Ketuk kapsul akan membuka aplikasi.", "Unlocked. Tapping the capsule opens the app.")
    val capsuleHiddenToast get() = t("Kapsul disembunyikan. Nyalakan lagi lewat tombol kapsul di kontrol media atau di Pengaturan.", "Capsule hidden. Turn it back on from the capsule button in media controls or in Settings.")
    val capsuleHideAction get() = t("Sembunyikan kapsul", "Hide capsule")
    val capsuleShowAction get() = t("Tampilkan kapsul", "Show capsule")
    val capsuleLockAction get() = t("Kunci kapsul", "Lock capsule")
    val capsuleUnlockAction get() = t("Buka kunci kapsul", "Unlock capsule")
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

    // Aksi cepat di pemutar
    val actionPlaylist get() = t("Playlist", "Playlist")
    val actionDownload get() = t("Unduh", "Download")
    val actionDownloaded get() = t("Terunduh", "Saved")
    val actionSleep get() = t("Timer tidur", "Sleep timer")
    val actionSpeed get() = t("Kecepatan", "Speed")
    val actionShare get() = t("Bagikan", "Share")
    val actionPlayNext get() = t("Putar next", "Play next")
    val sleepTitle get() = t("Timer tidur", "Sleep timer")
    val sleepSub get() = t("Musik berhenti otomatis setelah waktu yang dipilih", "Music stops automatically after the chosen time")
    fun sleepMinutes(m: Int) = t("$m menit", "$m minutes")
    val sleepEndOfTrack get() = t("Sampai lagu ini selesai", "Until this song ends")
    val sleepOff get() = t("Matikan timer", "Turn off timer")
    fun sleepSet(m: Int) = t("Musik berhenti dalam $m menit", "Music stops in $m minutes")
    val sleepSetEndOfTrack get() = t("Musik berhenti setelah lagu ini", "Music stops after this song")
    val sleepCancelled get() = t("Timer tidur dimatikan", "Sleep timer turned off")
    val sleepFired get() = t("Timer tidur selesai, musik dijeda", "Sleep timer finished, music paused")
    fun sleepLeft(m: Int) = t("Sisa $m mnt", "$m min left")
    val speedTitle get() = t("Kecepatan putar", "Playback speed")
    val speedNormal get() = t("Normal", "Normal")
    fun speedSet(x: String) = t("Kecepatan putar $x", "Playback speed $x")
    val noteSleepTitle get() = t("Timer tidur", "Sleep timer")
    val noteSpeedTitle get() = t("Kecepatan", "Speed")

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
    fun sectionTitle(raw: String): String {
        // Normalisasi variasi kapitalisasi/judul dari API agar teks Indonesia
        // tidak muncul saat bahasa aplikasi menggunakan English.
        val key = raw.trim().lowercase().replace(Regex("\\s+"), " ")
        return when (key) {
            "lagi sering kamu putar", "on repeat for you" -> t("Lagi Sering Kamu Putar", "On repeat for you")
            "pilihan cepat", "pilihan untukmu", "quick picks", "picks for you" -> t("Pilihan Cepat", "Quick picks")
            "lagu trending", "trending songs" -> t("Lagu Trending", "Trending songs")
            "video musik terbaru", "latest music videos", "music videos" -> t("Video Musik Terbaru", "Latest Music Videos")
            "mix lagu", "music mix", "music mixes", "mixes" -> t("Mix Lagu", "Music Mixes")
            else -> raw
        }
    }
    fun sectionSubtitle(raw: String) = when (raw) {
        "Berdasarkan selera musikmu" -> t(raw, "Based on your taste")
        "Dengarkan musik favoritmu" -> t(raw, "Listen to your favorite music")
        "Paling banyak diputar" -> t(raw, "Most played right now")
        else -> raw
    }
    fun period(raw: String) = when (raw) {
        "Maghrib" -> t("Selamat Maghrib", "Maghrib time")
        "Before Maghrib" -> t("Menjelang Maghrib", "Almost Maghrib")
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


    // Popup pemberitahuan
    val noteSuccess get() = t("Berhasil", "Done")
    val noteInfo get() = t("Info", "Info")
    val noteWarning get() = t("Perhatian", "Heads up")
    val noteError get() = t("Gagal", "Oops")
    val notePlaylistTitle get() = t("Masuk ke playlist", "Added to playlist")
    fun notePlaylistAdded(track: String, playlist: String) = t("\"$track\" sudah masuk ke playlist $playlist", "\"$track\" was added to $playlist")
    fun notePlaylistExists(playlist: String) = t("Lagu ini sudah ada di playlist $playlist", "This song is already in $playlist")
    fun notePlaylistCreated(playlist: String, track: String) = t("Playlist $playlist dibuat dan \"$track\" sudah masuk", "Playlist $playlist created with \"$track\"")
    fun notePlaylistDeleted(playlist: String) = t("Playlist $playlist dihapus", "Playlist $playlist deleted")
    val notePlaylistDeletedTitle get() = t("Playlist dihapus", "Playlist deleted")
    fun favAdded(track: String) = t("\"$track\" ditambahkan ke lagu favorit", "\"$track\" added to your favorites")
    fun favRemoved(track: String) = t("\"$track\" dihapus dari lagu favorit", "\"$track\" removed from your favorites")
    val favTitleAdded get() = t("Disukai", "Liked")
    val favTitleRemoved get() = t("Batal suka", "Unliked")
    fun notePlaylistRemoved(track: String, playlist: String) = t("\"$track\" dihapus dari playlist $playlist", "\"$track\" was removed from $playlist")
    val notePlaylistRemovedTitle get() = t("Dihapus dari playlist", "Removed from playlist")
    val playlistTapToRemove get() = t("Ketuk lagi untuk menghapus lagu dari playlist", "Tap again to remove the song from a playlist")
    fun songCount(n: Int) = t("$n lagu", "$n songs")
    val removeFromPlaylistCd get() = t("Hapus dari playlist", "Remove from playlist")
    val removeFromQueueAction get() = t("Hapus dari antrian", "Remove from queue")
    val toastQueueRemoved get() = t("Dihapus dari antrian", "Removed from queue")
    val toastQueueIsCurrent get() = t("Lagu ini sedang diputar", "This song is playing right now")
    val noteQueueTitle get() = t("Antrian", "Queue")
    val noteDownloadTitle get() = t("Unduhan", "Download")
    val noteCacheTitle get() = t("Penyimpanan", "Storage")
    val noteProfileTitle get() = t("Akun", "Account")
    val noteLocationTitle get() = t("Lokasi", "Location")
    val noteCapsuleTitle get() = t("Kapsul musik", "Music capsule")
    val noteOfflineTitle get() = t("Mode offline", "Offline mode")
    val noteUpdateTitle get() = t("Pembaruan", "Update")

    // Pengingat Maghrib
    val reminderSettingTitle get() = t("Pengingat sebelum Maghrib", "Pre-Maghrib reminder")
    val reminderSettingDesc get() = t(
        "Muncul popup sebelum azan Maghrib dan musik dijeda sementara. Bisa dimatikan kapan saja.",
        "Shows a popup before Maghrib and pauses your music for a moment. You can turn it off anytime."
    )
    val reminderTitleBefore get() = t("Maghrib sebentar lagi", "Maghrib is almost here")
    val reminderTitleNow get() = t("Waktu Maghrib telah tiba", "It's Maghrib time")
    fun reminderBody(time: String, now: Boolean) = if (now) t(
        "Waktu Maghrib sudah masuk (sekitar pukul $time di lokasimu). Bagi yang beragama Islam, silakan berhenti sejenak dan tunaikan salat Maghrib. Bagi yang non-Muslim, silakan lanjutkan aktivitasmu dengan nyaman. Salam toleransi untuk kita semua. 🤍",
        "Maghrib has begun (around $time at your location). For those who observe it, please pause and make time for the Maghrib prayer. For everyone else, please carry on comfortably. Peace and respect to all of us. 🤍"
    ) else t(
        "Waktu Maghrib hampir tiba, sekitar pukul $time di lokasimu. Bagi yang beragama Islam, silakan berhenti sejenak dan bersiap salat Maghrib. Bagi yang non-Muslim, silakan lanjutkan aktivitasmu dengan nyaman. Salam toleransi untuk kita semua. 🤍",
        "Maghrib is almost here, around $time at your location. For those who observe it, please pause and get ready for the Maghrib prayer. For everyone else, please carry on comfortably. Peace and respect to all of us. 🤍"
    )
    val reminderMusicPaused get() = t("Musik dijeda selama popup ini tampil.", "Your music is paused while this popup is open.")
    val reminderClose get() = t("TUTUP", "CLOSE")
    val reminderExit get() = t("KELUAR", "EXIT")
    val reminderCloseHint get() = t("TUTUP melanjutkan musik. KELUAR menutup aplikasi dan musik tetap berhenti sampai kamu menyalakannya lagi.", "CLOSE resumes your music. EXIT closes the app and keeps the music stopped until you start it again.")

    // Izin lokasi untuk Maghrib
    val locPromptTitle get() = t("Izinkan lokasi untuk Maghrib", "Allow location for Maghrib")
    val locPromptBody get() = t(
        "Nexrify butuh lokasimu untuk menghitung waktu Maghrib yang pas di kotamu, lalu menampilkan sapaan dan pengingat sebelum azan. Lokasi cuma dipakai di HP dan tidak dikirim ke mana pun.",
        "Nexrify needs your location to work out the exact Maghrib time in your city and show the greeting and reminder before the call to prayer. Your location stays on your phone and is never sent anywhere."
    )
    val locPromptAllow get() = t("Izinkan lokasi", "Allow location")
    val locPromptSettings get() = t("Buka pengaturan izin", "Open permission settings")
    val locPromptSettingsHint get() = t("Izin lokasi ditolak permanen. Buka pengaturan aplikasi lalu aktifkan Lokasi.", "Location was denied permanently. Open app settings and turn on Location.")
    val locPromptLater get() = t("Nanti saja", "Maybe later")
    val locGranted get() = t("Lokasi aktif, jadwal Maghrib sedang diperbarui", "Location is on, updating your Maghrib time")

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
        "Wajib izinkan lokasi. Waktu Maghrib dihitung langsung dari terbenam matahari di kotamu, jadi pas dan ikut berubah kalau kamu pindah tempat.",
        "Location access is required. Maghrib is calculated from the sunset in your city, so it stays accurate and follows you when you move."
    )
    val maghribNoPermission get() = t(
        "Izin lokasi belum diberikan, jadi sapaan dan pengingat Maghrib belum aktif. Musik tetap bisa dipakai seperti biasa.",
        "Location permission isn't granted, so the Maghrib greeting and reminder are off. Music works as usual."
    )
    val maghribLocating get() = t("Lagi nyari lokasimu…", "Finding your location…")
    val maghribUnavailable get() = t(
        "Lokasi belum bisa didapat, jadi sapaan Maghrib dimatikan dulu. Nyalakan lokasi HP lalu coba lagi.",
        "Couldn't get your location, so the Maghrib greeting is off for now. Turn on location and try again."
    )
    fun maghribReady(time: String) = t("Maghrib hari ini sekitar $time di lokasimu.", "Maghrib today is around $time at your location.")
    val maghribNote get() = t("Dihitung dari terbenam matahari di lokasimu, pas sampai ke menit. Bisa selisih sekitar 1 menit dari jadwal resmi.", "Calculated from the sunset at your location, accurate to the minute. May differ by about 1 minute from the official schedule.")
    val maghribGrant get() = t("Izinkan lokasi", "Allow location")
    val maghribRetry get() = t("Coba lagi", "Try again")
    val maghribHomeHint get() = t("Izinkan lokasi supaya sapaan dan pengingat Maghrib pas dengan jadwal di kotamu.", "Allow location so the Maghrib greeting and reminder match your city's schedule.")
    val googleUser get() = t("Pengguna Google", "Google user")

    val likeAction get() = t("Suka", "Like")
    val unlikeAction get() = t("Batal suka", "Unlike")

    // Laporan crash
    val crashTitle get() = t("Nexrify sempat berhenti", "Nexrify stopped unexpectedly")
    val crashBody get() = t("Maaf, aplikasi tadi berhenti tiba tiba. Salin atau bagikan laporan ini ke pengembang supaya masalahnya cepat diperbaiki.", "Sorry, the app stopped unexpectedly. Copy or share this report with the developer so it can be fixed quickly.")
    val crashCopy get() = t("Salin laporan", "Copy report")
    val crashShare get() = t("Bagikan", "Share")
    val crashCopied get() = t("Laporan disalin", "Report copied")

    // Akun
    val authSubtitle get() = t("Masuk untuk menyimpan favorit, playlist, dan riwayatmu di akunmu.", "Sign in to keep your favorites, playlists, and history with your account.")
    val authLogin get() = t("Masuk", "Sign in")
    val authRegister get() = t("Daftar", "Sign up")
    val authCreate get() = t("Buat akun", "Create account")
    val fieldUsername get() = t("Username", "Username")
    val fieldDisplay get() = t("Nama tampilan", "Display name")
    val fieldPassword get() = t("Password", "Password")
    val fieldOldPassword get() = t("Password lama", "Current password")
    val fieldNewPassword get() = t("Password baru", "New password")
    val usernameHint get() = t("3 sampai 20 karakter: huruf kecil, angka, atau garis bawah", "3 to 20 characters: lowercase letters, numbers, or underscore")
    val passwordShow get() = t("Tampilkan", "Show")
    val passwordHide get() = t("Sembunyikan", "Hide")
    val authAgree get() = t("Dengan melanjutkan, kamu menyetujui Syarat penggunaan dan Kebijakan privasi Nexrify.", "By continuing, you agree to Nexrify's Terms of use and Privacy policy.")
    val authTerms get() = t("Syarat", "Terms")
    val authPrivacy get() = t("Privasi", "Privacy")
    val authSyncing get() = t("Menyinkronkan data akunmu…", "Syncing your account data…")
    fun accountError(code: String) = when (code) {
        "invalid_username" -> t("Username harus 3 sampai 20 karakter: huruf kecil, angka, atau garis bawah.", "Username must be 3 to 20 characters: lowercase letters, numbers, or underscore.")
        "username_taken" -> t("Username sudah dipakai. Coba yang lain.", "That username is taken. Try another one.")
        "weak_password" -> t("Password harus 6 sampai 72 karakter.", "Password must be 6 to 72 characters.")
        "bad_credentials" -> t("Username atau password salah.", "Wrong username or password.")
        "banned" -> t("Akun ini dinonaktifkan.", "This account has been disabled.")
        "rate_limited" -> t("Terlalu banyak percobaan. Coba lagi beberapa menit lagi.", "Too many attempts. Try again in a few minutes.")
        "network" -> t("Tidak ada koneksi internet. Periksa koneksimu lalu coba lagi.", "No internet connection. Check your connection and try again.")
        "invalid_display" -> t("Nama tampilan tidak boleh kosong.", "Display name can't be empty.")
        "invalid_avatar" -> t("Foto tidak valid atau terlalu besar.", "The photo is invalid or too large.")
        "auth" -> t("Sesi berakhir, silakan masuk lagi.", "Your session expired, please sign in again.")
        else -> t("Layanan sedang bermasalah. Coba lagi sebentar.", "The service is having trouble. Try again shortly.")
    }
    val editProfile get() = t("Edit profil", "Edit profile")
    val changePhoto get() = t("Ganti foto", "Change photo")
    val removePhoto get() = t("Hapus foto", "Remove photo")
    val save get() = t("Simpan", "Save")
    val profileSaved get() = t("Profil disimpan", "Profile saved")
    val changePassword get() = t("Ganti password", "Change password")
    val passwordChanged get() = t("Password diubah", "Password changed")
    val logoutAccount get() = t("Keluar dari akun", "Sign out")
    val logoutTitle get() = t("Keluar dari akun?", "Sign out?")
    val logoutBody get() = t("Datamu sudah tersimpan di akun. Unduhan offline tetap aman di perangkat dan muncul lagi saat kamu masuk.", "Your data is saved to your account. Offline downloads stay safe on this device and come back when you sign in.")
    val youtubeOptional get() = t("YouTube Music (opsional)", "YouTube Music (optional)")
    val youtubeConnectHint get() = t("Hubungkan untuk rekomendasi dan koleksi YouTube", "Connect for YouTube recommendations and library")
    val youtubeDisconnect get() = t("Putuskan", "Disconnect")
    fun mailboxFailedDetail(detail: String) = t("Detail: $detail", "Details: $detail")
    val mailChannel get() = t("Kotak surat", "Mailbox")
    fun mailNotifMore(n: Int) = t("dan $n surat lainnya", "and $n more messages")
    val confirmYes get() = t("Ya", "Yes")

    // Update
    val updateDialogTitle get() = t("Update tersedia", "Update available")
    val updateWhatsNew get() = t("Yang baru:", "What's new:")
    val updateNoNotes get() = t("Perbaikan dan peningkatan performa.", "Fixes and performance improvements.")
    val updateLater get() = t("Nanti", "Later")
    val updateNow get() = t("Update sekarang", "Update now")

    // Kotak surat
    val mailboxTitle get() = t("Kotak surat", "Mailbox")
    val mailboxSubtitle get() = t("Kabar resmi dari Nexrify", "Official news from Nexrify")
    val mailboxMarkAll get() = t("Tandai dibaca", "Mark all read")
    val mailboxEmpty get() = t("Belum ada surat", "No messages yet")
    val mailboxEmptyHint get() = t("Info, update, dan kabar penting dari Nexrify akan muncul di sini.", "Info, updates, and important news from Nexrify will show up here.")
    val mailboxFailed get() = t("Kotak surat belum bisa dimuat", "Couldn't load the mailbox")
    val mailboxFailedHint get() = t("Periksa koneksi internetmu lalu buka lagi halaman ini.", "Check your connection and open this page again.")
    val mailboxPinned get() = t("DISEMATKAN", "PINNED")
    val mailboxOpenLink get() = t("Buka tautan", "Open link")
    fun mailType(type: String) = when (type) {
        "update" -> t("UPDATE", "UPDATE")
        "promo" -> t("PROMO", "PROMO")
        "alert" -> t("PENTING", "IMPORTANT")
        else -> t("INFO", "INFO")
    }

    // Beranda baru
    val playAgain get() = t("Putar lagi", "Play again")
    val picksForYou get() = t("Pilihan untukmu", "Picks for you")
    val personalMix get() = t("MIX PRIBADI", "PERSONAL MIX")
    val viewAll get() = t("Lihat semua", "View all")
    val showLess get() = t("Ciutkan", "Show less")

    // Mode offline
    val offlineNotDownloaded get() = t("Lagu ini belum diunduh dan kamu lagi offline", "This song isn't downloaded and you're offline")
    val offlineBanner get() = t("Lagi offline. Lagu yang sudah diunduh tetap bisa diputar.", "You're offline. Downloaded songs still play.")
    val offlineBannerAction get() = t("Buka Offline", "Open Offline")

    // Unduhan offline
    val offlineTab get() = t("Offline", "Offline")

    // Pencarian di Koleksi (lagu disukai, playlist, offline)
    val searchLikedHint get() = t("Cari di lagu disukai…", "Search liked songs…")
    val searchPlaylistsHint get() = t("Cari playlist atau lagu di dalamnya…", "Search playlists or songs in them…")
    val searchPlaylistSongsHint get() = t("Cari lagu di playlist ini…", "Search songs in this playlist…")
    val searchOfflineHint get() = t("Cari di lagu offline…", "Search offline songs…")
    val clearSearchCd get() = t("Hapus pencarian", "Clear search")
    fun noMatchFor(q: String) = t("Nggak ada hasil buat \"$q\"", "No results for \"$q\"")
    val noMatchHint get() = t("Coba kata kunci lain, bisa judul atau nama artis.", "Try another keyword, a title or an artist name works.")
    fun matchCount(n: Int) = t("$n hasil", "$n results")
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
            "Show player" -> "Tampilkan pemutar"
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

    // ===== Global Chat =====
    val chatTitle get() = t("Global Chat", "Global Chat")
    val chatSubtitle get() = t("Ruang obrolan semua pengguna Nexrify", "Chat room for all Nexrify users")
    val chatConnecting get() = t("Menyambungkan…", "Connecting…")
    val chatOffline get() = t("Tidak terhubung. Mencoba lagi…", "Not connected. Retrying…")
    val chatEmptyTitle get() = t("Belum ada obrolan", "No messages yet")
    val chatEmptySub get() = t("Jadilah yang pertama menyapa pengguna Nexrify lainnya.", "Be the first to say hi to other Nexrify users.")
    val chatInputHint get() = t("Tulis pesan…", "Write a message…")
    val chatEditing get() = t("Mengedit pesan", "Editing message")
    fun chatReplyingTo(name: String) = t("Membalas $name", "Replying to $name")
    val chatEdited get() = t("diedit", "edited")
    val chatDeletedMsg get() = t("Pesan ini telah dihapus", "This message was deleted")
    val chatRulesBanner get() = t("Hanya teks dan emoji. Kata kasar disensor otomatis jadi ####.", "Text and emoji only. Rude words are censored automatically as ####.")
    val chatNewMessages get() = t("Pesan baru", "New messages")
    val chatReplyAction get() = t("Balas", "Reply")
    val chatCopyAction get() = t("Salin", "Copy")
    val chatEditAction get() = t("Edit", "Edit")
    val chatDeleteAction get() = t("Hapus", "Delete")
    val chatReportAction get() = t("Laporkan", "Report")
    val chatProfileAction get() = t("Lihat profil", "View profile")
    val chatMuteAction get() = t("Mute pengguna", "Mute user")
    val chatUnmuteAction get() = t("Buka mute", "Unmute")
    val chatCopied get() = t("Pesan disalin", "Message copied")
    val chatDeleteTitle get() = t("Hapus pesan?", "Delete message?")
    val chatDeleteBody get() = t("Pesan ini akan dihapus untuk semua orang dan tidak bisa dikembalikan.", "This message will be deleted for everyone and can't be restored.")
    val chatShareAction get() = t("Bagikan", "Share")
    val chatMoreReactions get() = t("Reaksi lainnya", "More reactions")
    val chatLessReactions get() = t("Tutup reaksi", "Hide reactions")
    val chatReactTitle get() = t("Pilih reaksi", "Pick a reaction")
    val chatReportTitle get() = t("Laporkan pesan", "Report message")
    val chatReportSpam get() = t("Spam", "Spam")
    val chatReportRude get() = t("Kata kasar atau pelecehan", "Rude words or harassment")
    val chatReportMisc get() = t("Lainnya", "Other")
    val chatReportDone get() = t("Laporan terkirim. Terima kasih.", "Report sent. Thank you.")
    val chatFailed get() = t("Gagal terkirim. Ketuk untuk kirim ulang.", "Failed to send. Tap to retry.")
    val chatSending get() = t("Mengirim…", "Sending…")
    fun chatTypingOne(a: String) = t("$a sedang mengetik…", "$a is typing…")
    fun chatTypingTwo(a: String, b: String) = t("$a dan $b sedang mengetik…", "$a and $b are typing…")
    fun chatTypingMany(n: Int) = t("$n orang sedang mengetik…", "$n people are typing…")
    val chatToday get() = t("Hari ini", "Today")
    val chatYesterday get() = t("Kemarin", "Yesterday")
    val chatFindPeople get() = t("Cari pengguna", "Find people")
    val chatFindHint get() = t("Ketik username…", "Type a username…")
    val chatFindEmpty get() = t("Tidak ada pengguna yang cocok", "No matching users")
    val chatFindStart get() = t("Cari teman lewat username untuk melihat profilnya.", "Search a friend by username to see their profile.")
    val chatSettings get() = t("Pengaturan chat", "Chat settings")
    val chatNotifAll get() = t("Notifikasi chat", "Chat notifications")
    val chatNotifAllSub get() = t("Matikan untuk berhenti menerima semua notifikasi chat di HP", "Turn off to stop all chat notifications on your phone")
    val chatNotifMention get() = t("Mention", "Mentions")
    val chatNotifMentionSub get() = t("Saat seseorang menyebut @usernamemu", "When someone mentions @yourusername")
    val chatNotifReply get() = t("Balasan", "Replies")
    val chatNotifReplySub get() = t("Saat pesanmu dibalas", "When your message gets a reply")
    val chatNotifReaction get() = t("Reaksi", "Reactions")
    val chatNotifReactionSub get() = t("Saat pesanmu diberi reaksi emoji", "When your message gets an emoji reaction")
    val chatNotifAnnounce get() = t("Pengumuman resmi", "Official announcements")
    val chatNotifAnnounceSub get() = t("Pengumuman dari tim Nexrify", "Announcements from the Nexrify team")
    val chatNotifLimitNote get() = t("Saat Global Chat sedang dibuka, notifikasi HP tidak dikirim supaya tidak spam. Maksimal satu notifikasi per jenis tiap beberapa menit.", "While Global Chat is open, phone notifications are not sent to avoid spam. At most one notification per type every few minutes.")
    val chatMutedUsers get() = t("Pengguna yang di-mute", "Muted users")
    val chatNoMuted get() = t("Belum ada pengguna yang di-mute", "No muted users yet")
    fun chatMuted(name: String) = t("$name di-mute. Pesannya disembunyikan.", "$name muted. Their messages are hidden.")
    fun chatUnmuted(name: String) = t("Mute $name dibuka", "$name unmuted")
    val chatNotifChannelMention get() = t("Mention Global Chat", "Global Chat mentions")
    val chatNotifChannelReply get() = t("Balasan Global Chat", "Global Chat replies")
    val chatNotifChannelReaction get() = t("Reaksi Global Chat", "Global Chat reactions")
    val chatNotifChannelAnnounce get() = t("Pengumuman Nexrify", "Nexrify announcements")
    fun chatNotifMentionTitle(from: String) = t("$from menyebutmu", "$from mentioned you")
    fun chatNotifReplyTitle(from: String) = t("$from membalas pesanmu", "$from replied to you")
    fun chatNotifReactionTitle(from: String) = t("$from memberi reaksi", "$from reacted")
    fun chatNotifReactionBody(emoji: String) = t("Memberi reaksi $emoji pada pesanmu", "Reacted $emoji to your message")
    val chatNotifAnnounceTitle get() = t("Pengumuman Nexrify", "Nexrify announcement")
    fun chatMentionsForYou(n: Int) = t("$n sebutan untukmu", "$n mentions for you")
    val chatProfileTitle get() = t("Profil", "Profile")
    val chatJoined get() = t("Bergabung", "Joined")
    val chatLikedSongs get() = t("Lagu disukai", "Liked songs")
    val chatPlaylistsStat get() = t("Playlist", "Playlists")
    val chatMessagesStat get() = t("Pesan", "Messages")
    val chatReactionsStat get() = t("Reaksi diterima", "Reactions received")
    val chatTopPlayed get() = t("Sering diputar", "Frequently played")
    val chatNoTop get() = t("Belum ada data lagu", "No song data yet")
    val chatBadges get() = t("Lencana", "Badges")
    val chatBadgeStaff get() = t("Staf", "Staff")
    val chatBadgeEarly get() = t("Anggota awal", "Early member")
    val chatBadgeActive get() = t("Aktif", "Active")
    val chatBadgeLoved get() = t("Disukai", "Loved")
    val chatNoBio get() = t("Belum ada bio", "No bio yet")
    val chatNoStatus get() = t("Belum ada status", "No status yet")
    val chatEditProfile get() = t("Edit profil", "Edit profile")
    val chatBioLabel get() = t("Bio singkat", "Short bio")
    val chatStatusLabel get() = t("Status", "Status")
    val chatSave get() = t("Simpan", "Save")
    val chatProfileSaved get() = t("Profil diperbarui", "Profile updated")
    val chatMention get() = t("Sebut", "Mention")
    val chatMe get() = t("kamu", "you")
    val chatBannedMsg get() = t("Kamu dibatasi mengirim pesan di Global Chat", "You are restricted from sending messages in Global Chat")
    val chatSlowDown get() = t("Terlalu cepat, tunggu sebentar", "Too fast, please wait a moment")
    val chatDuplicate get() = t("Pesan yang sama baru saja dikirim", "That same message was just sent")
    val chatEditExpired get() = t("Pesan hanya bisa diedit dalam 1 jam", "Messages can only be edited within 1 hour")
    val chatGenericError get() = t("Gagal, coba lagi sebentar", "Something went wrong, try again shortly")
    val chatNoNetwork get() = t("Tidak ada koneksi internet", "No internet connection")
    val chatNotifTitle get() = t("Notifikasi", "Notifications")
    val chatAnnouncement get() = t("Pengumuman", "Announcement")
    val chatJumpToOriginal get() = t("Pesan asli tidak ada lagi di riwayat", "The original message is no longer in the history")
    val screenshotBlocked get() = t("Tidak dapat menangkap layar dikarenakan keamanan pribadi", "Can't capture the screen because of personal privacy")
    val avatarProtectedNote get() = t("Foto ini dilindungi. Screenshot dan rekam layar dinonaktifkan demi keamanan pribadi.", "This photo is protected. Screenshots and screen recording are disabled for personal privacy.")
    val chatBadgeAdmin get() = t("Admin", "Admin")
    val chatBadgeDeveloper get() = t("Developer", "Developer")
    val chatOnline get() = t("ONLINE", "ONLINE")
    val chatOffline2 get() = t("OFFLINE", "OFFLINE")
    val chatGroupInfo get() = t("Info grup", "Group info")
    fun chatMembersCount(n: Int) = t("$n anggota", "$n members")
    fun chatMembersOnline(n: Int, on: Int) = t("$n anggota, $on online", "$n members, $on online")
    val chatMembersTitle get() = t("Anggota", "Members")
    fun chatOnlineNow(n: Int) = t("$n online sekarang", "$n online now")
    val chatSearchMembers get() = t("Cari anggota…", "Search members…")
    val chatNoMembers get() = t("Belum ada anggota yang cocok", "No matching members")
    val chatGroupDescLabel get() = t("Deskripsi", "Description")
    val chatGroupNameLabel get() = t("Nama grup", "Group name")
    val chatEditGroup get() = t("Edit info grup", "Edit group info")
    val chatChangePhoto get() = t("Ganti foto grup", "Change group photo")
    val chatRemovePhoto get() = t("Hapus foto", "Remove photo")
    val chatGroupSaved get() = t("Info grup diperbarui", "Group info updated")
    val chatGroupForbidden get() = t("Hanya admin dan staf yang bisa mengubah info grup", "Only admins and staff can change group info")
    val chatGroupNameEmpty get() = t("Nama grup tidak boleh kosong", "Group name can't be empty")
    val chatStaffAdminSection get() = t("Admin & Staf", "Admins & Staff")
    val chatMemberSection get() = t("Anggota", "Members")
    val chatPhotoInvalid get() = t("Foto tidak bisa dipakai", "That photo can't be used")
    val chatOnlineStatus get() = t("Status", "Status")
    fun chatNotifMore(n: Int) = t("dan $n lainnya", "and $n more")
    val chatOpenChat get() = t("Buka Global Chat", "Open Global Chat")

    // ===== Pesan langsung (DM) =====
    val dmTitle get() = t("Pesan", "Messages")
    val dmEmptyTitle get() = t("Belum ada pesan", "No messages yet")
    val dmEmptySub get() = t("Cari teman lewat username lalu kirim pesan langsung.", "Find a friend by username and send a direct message.")
    val dmNewMessage get() = t("Pesan baru", "New message")
    val dmYou get() = t("Kamu", "You")
    val dmTyping get() = t("sedang mengetik…", "typing…")
    val dmRead get() = t("Dibaca", "Read")
    val dmSendMessage get() = t("Kirim pesan", "Send message")
    val dmBlockUser get() = t("Blokir pengguna", "Block user")
    val dmUnblockUser get() = t("Buka blokir", "Unblock")
    val dmBlockedBanner get() = t("Kamu memblokir pengguna ini", "You blocked this user")
    val dmBlockedDone get() = t("Pengguna diblokir. Dia tidak bisa mengirim pesan langsung kepadamu.", "User blocked. They can no longer send you direct messages.")
    val dmUnblockedDone get() = t("Blokir dibuka", "User unblocked")
    val dmErrBlocked get() = t("Pengguna ini tidak menerima pesan darimu", "This user does not accept messages from you")
    val dmErrYouBlocked get() = t("Kamu memblokir pengguna ini. Buka blokirnya dulu.", "You blocked this user. Unblock them first.")
    val dmLoadFailed get() = t("Tidak bisa memuat percakapan", "Could not load the conversation")
    val dmStartHint get() = t("Mulai percakapan dengan menulis pesan pertama.", "Start the conversation by writing the first message.")
    val dmOpenMessages get() = t("Buka pesan", "Open messages")
    fun dmNotifTitle(from: String) = t("$from mengirim pesan", "$from sent you a message")
    val dmNotifTitleMany get() = t("Pesan langsung baru", "New direct messages")
    val chatNotifChannelDm get() = t("Pesan langsung", "Direct messages")
    val chatNotifDm get() = t("Pesan langsung", "Direct messages")
    val chatNotifDmSub get() = t("Saat seseorang mengirim pesan langsung kepadamu", "When someone sends you a direct message")
    val dmMore get() = t("Lainnya", "More")

    // ===== Donasi / Dukung Developer =====
    val donateTitle get() = t("Dukung Developer", "Support the Developer")
    val donateSubtitle get() = t("Donasi sukarela", "Voluntary donation")
    val donateEntrySub get() = t("Donasi sukarela · lihat Top Donatur", "Voluntary donation · see Top Donors")
    val donateHeroTitle get() = t("Traktir developer", "Treat the developer")
    val donateHeroBody get() = t("Donasi tidak wajib dan sepenuhnya sukarela. Dukunganmu bikin admin semangat terus update Nexrify ke depannya.", "Donating is optional and completely voluntary. Your support keeps the admin motivated to keep updating Nexrify.")
    val donateNow get() = t("Donasi sekarang", "Donate now")
    val donateClosed get() = t("Donasi sedang ditutup", "Donations are closed right now")
    val donateTopTitle get() = t("Top Donatur", "Top Donors")
    val donateTopEmpty get() = t("Belum ada donatur", "No donors yet")
    val donateTopEmptySub get() = t("Jadilah yang pertama dan raih title TOP 1 DONATE.", "Be the first and earn the TOP 1 DONATE title.")
    fun donateTag(rank: Int) = t("TOP $rank DONATE", "TOP $rank DONATE")
    val donateTotalLabel get() = t("Total donasi", "Total donated")
    val donateMethod get() = t("Metode donasi", "Donation method")
    val donateQris get() = t("QRIS", "QRIS")
    val donateDana get() = t("DANA", "DANA")
    val donateQrisSub get() = t("Scan QRIS statis dengan e-wallet atau m-banking", "Scan the static QRIS with any e-wallet or m-banking")
    val donateDanaSub get() = t("Transfer ke nomor DANA owner", "Transfer to the owner's DANA number")
    val donateDanaNotReady get() = t("Belum tersedia", "Not available yet")
    val donateAmount get() = t("Nominal donasi", "Donation amount")
    val donateCustomAmount get() = t("Nominal lain", "Other amount")
    fun donateMin(rp: String) = t("Minimal $rp", "Minimum $rp")
    val donateDonor get() = t("Akun donatur", "Donor account")
    val donateDonorSub get() = t("Terdeteksi otomatis dan ikut dikirim ke owner", "Detected automatically and sent to the owner")
    val donateSenderLabel get() = t("Nama pengirim", "Sender name")
    val donateSenderHint get() = t("Nama di akun e-wallet atau rekening yang dipakai", "Name on the e-wallet or bank account you pay with")
    val donateNoteLabel get() = t("Pesan untuk developer (opsional)", "Message for the developer (optional)")
    val donateContinue get() = t("Lanjut ke pembayaran", "Continue to payment")
    val donateNeedSender get() = t("Isi nama pengirim dulu", "Enter the sender name first")
    fun donateNeedAmount(rp: String) = t("Nominal minimal $rp", "Minimum amount is $rp")
    val donateDetailTitle get() = t("Detail donasi", "Donation details")
    val donateStatusPending get() = t("Menunggu bukti", "Waiting for proof")
    val donateStatusReview get() = t("Diperiksa owner", "Owner reviewing")
    val donateStatusPaid get() = t("Berhasil", "Successful")
    val donateStatusRejected get() = t("Ditolak", "Rejected")
    val donateStatusCancelled get() = t("Dibatalkan", "Cancelled")
    val donateStepPay get() = t("1. Bayar sesuai nominal", "1. Pay the exact amount")
    val donateStepProof get() = t("2. Kirim bukti", "2. Send the proof")
    fun donateTransferExact(rp: String) = t("Transfer tepat $rp", "Transfer exactly $rp")
    val donateScanQris get() = t("Scan QRIS ini", "Scan this QRIS")
    val donateQrisFail get() = t("Gambar QRIS belum bisa dimuat", "The QRIS image can't be loaded yet")
    val donateOpenQris get() = t("Buka QRIS di browser", "Open QRIS in browser")
    val donateDanaName get() = t("Atas nama", "Account name")
    val donateDanaNumber get() = t("Nomor DANA", "DANA number")
    val donateCopy get() = t("Salin", "Copy")
    val donateCopied get() = t("Nomor DANA disalin", "DANA number copied")
    val donateProofHint get() = t("Unggah screenshot, foto, video, atau file bukti transfer yang menunjukkan pembayaran berhasil. Maksimal 15 MB.", "Upload a screenshot, photo, video, or file showing the payment succeeded. 15 MB max.")
    val donatePickProof get() = t("Pilih bukti", "Choose proof")
    val donateChangeProof get() = t("Ganti bukti", "Change proof")
    val donateSendProof get() = t("Kirim bukti", "Send proof")
    val donateProofSent get() = t("Bukti terkirim. Owner akan segera memeriksa.", "Proof sent. The owner will check it soon.")
    val donateProofInvalid get() = t("Bukti tidak bisa dibaca, pilih file lain", "That file can't be read, choose another one")
    val donateProofTooBig get() = t("File terlalu besar, maksimal 15 MB", "That file is too large, 15 MB max")
    val donateReviewInfo get() = t("Bukti sudah dikirim ke owner. Status berubah otomatis setelah diperiksa.", "Your proof was sent to the owner. The status updates automatically once checked.")
    val donatePaidInfo get() = t("Donasi diterima. Terima kasih sudah mendukung Nexrify!", "Donation received. Thank you for supporting Nexrify!")
    val donateRejectedInfo get() = t("Bukti ditolak owner. Donasi ini tidak dihitung.", "The owner rejected the proof. This donation isn't counted.")
    val donateCancelledInfo get() = t("Donasi ini dibatalkan dan tidak bisa dilanjutkan lagi.", "This donation was cancelled and can't be continued.")
    val donateCancel get() = t("Batalkan donasi", "Cancel donation")
    val donateCancelTitle get() = t("Batalkan donasi?", "Cancel donation?")
    val donateCancelBody get() = t("Setelah dibatalkan, donasi ini tidak bisa dilanjutkan lagi.", "Once cancelled, this donation can't be continued.")
    val donateCancelDone get() = t("Donasi dibatalkan", "Donation cancelled")
    val donateKeep get() = t("Kembali", "Back")
    val donateHistory get() = t("Riwayat donasi", "Donation history")
    val donateHistoryEmpty get() = t("Belum ada riwayat donasi", "No donation history yet")
    val donateContinuePay get() = t("Lanjutkan kirim bukti", "Continue sending proof")
    val donateDeleteHistory get() = t("Hapus riwayat", "Delete history")
    val donateDeleteTitle get() = t("Hapus riwayat ini?", "Delete this history entry?")
    val donateDeleteBody get() = t("Hanya hilang dari daftarmu. Total donasi di Top Donatur tidak berubah.", "It only disappears from your list. Totals on the Top Donors board don't change.")
    val donateDeleteAll get() = t("Hapus yang selesai", "Clear finished")
    val donateDeleteAllBody get() = t("Semua riwayat yang sudah selesai (berhasil, ditolak, dibatalkan) dihapus dari daftarmu.", "All finished history (successful, rejected, cancelled) is removed from your list.")
    val donateDeleted get() = t("Riwayat dihapus", "History deleted")
    val donatePaidToast get() = t("Donasi diterima! Terima kasih", "Donation received! Thank you")
    val donateMyTitle get() = t("Title profil", "Profile title")
    fun donateYouAre(rank: Int) = t("Kamu TOP $rank DONATE", "You are TOP $rank DONATE")
    val donateTitleSwitch get() = t("Pasang title di profil dan chat", "Show title on profile and chat")
    val donateGenericError get() = t("Gagal memproses donasi, coba lagi", "Couldn't process the donation, try again")
    val donateRefresh get() = t("Muat ulang", "Refresh")
    val donateProfileTitleSection get() = t("Title", "Title")
    val donateOpenCd get() = t("Buka halaman donasi", "Open the donation page")

    // ===== Fitur chat lanjutan (4.0.7) =====
    val chatReceiptDelivered get() = t("Tersampaikan", "Delivered")
    fun chatReceiptRead(n: Int) = t("Dibaca $n", "Read by $n")
    val chatSearchMessages get() = t("Cari pesan", "Search messages")
    val chatSearchTabMessages get() = t("Pesan", "Messages")
    val chatSearchTabPeople get() = t("Pengguna", "People")
    val chatSearchKeyword get() = t("Kata kunci", "Keyword")
    val chatSearchUser get() = t("Username pengirim", "Sender username")
    val chatSearchAnyDate get() = t("Semua tanggal", "Any date")
    val chatSearchPickDate get() = t("Pilih tanggal", "Pick a date")
    val chatSearchStartHint get() = t("Ketik kata kunci, isi username, atau pilih tanggal.", "Type a keyword, enter a username, or pick a date.")
    val chatSearchNoResult get() = t("Tidak ada pesan yang cocok", "No matching messages")
    fun chatSearchCount(n: Int) = t("$n pesan ditemukan", "$n messages found")
    val chatSearchOld get() = t("Pesan ini tidak ada di riwayat yang sedang dimuat", "This message isn't in the loaded history")
    val chatTranslateId get() = t("Terjemahkan ke Indonesia", "Translate to Indonesian")
    val chatTranslateEn get() = t("Terjemahkan ke Inggris", "Translate to English")
    val chatTranslated get() = t("Terjemahan", "Translation")
    val chatHideTranslation get() = t("Sembunyikan terjemahan", "Hide translation")
    val chatTranslateFail get() = t("Terjemahan gagal, coba lagi", "Translation failed, try again")
    val chatTranslating get() = t("Menerjemahkan…", "Translating…")
    val chatJustNow get() = t("baru saja", "just now")
    fun chatAgoMin(n: Int) = t("$n mnt lalu", "$n min ago")
    fun chatAgoHour(n: Int) = t("$n jam lalu", "$n h ago")
    fun chatAgoDay(n: Int) = t("$n hari lalu", "$n d ago")
    fun chatLastSeen(ago: String) = t("Terakhir aktif $ago", "Last active $ago")
    val chatPresenceHidden get() = t("Status online disembunyikan", "Online status hidden")
    val chatPollCreate get() = t("Buat polling", "Create poll")
    val chatPollQuestion get() = t("Pertanyaan polling", "Poll question")
    fun chatPollOption(n: Int) = t("Pilihan $n", "Option $n")
    val chatPollAddOption get() = t("Tambah pilihan", "Add option")
    val chatPollDuration get() = t("Durasi", "Duration")
    val chatPollNoLimit get() = t("Tanpa batas", "No limit")
    fun chatPollHours(n: Int) = t("$n jam", "$n h")
    fun chatPollDays(n: Int) = t("$n hari", "$n d")
    val chatPollPublish get() = t("Kirim polling", "Post poll")
    fun chatPollVotes(n: Int) = t("$n suara", "$n votes")
    val chatPollClosed get() = t("Polling ditutup", "Poll closed")
    val chatPollClose get() = t("Tutup polling", "Close poll")
    val chatPollLabel get() = t("POLLING", "POLL")
    val chatPollNeed get() = t("Isi pertanyaan dan minimal 2 pilihan", "Enter a question and at least 2 options")
    val chatPollVoted get() = t("Suaramu tercatat", "Your vote is recorded")
    val chatPollTapHint get() = t("Ketuk pilihan untuk memberi suara. Kamu bisa mengubahnya sampai polling ditutup.", "Tap an option to vote. You can change it until the poll closes.")
    val chatPinAction get() = t("Sematkan", "Pin")
    val chatUnpinAction get() = t("Lepas sematan", "Unpin")
    val chatPinnedTitle get() = t("Pesan disematkan", "Pinned message")
    fun chatPinnedOf(i: Int, n: Int) = t("$i dari $n", "$i of $n")
    val chatViewReactions get() = t("Lihat reaksi", "View reactions")
    val chatReactionsTitle get() = t("Reaksi", "Reactions")
    fun chatReactionsTotal(n: Int) = t("$n reaksi", "$n reactions")
    val chatNoReactions get() = t("Belum ada reaksi", "No reactions yet")
    val chatThreadAction get() = t("Buka thread", "Open thread")
    val chatThreadTitle get() = t("Thread", "Thread")
    fun chatRepliesCount(n: Int) = t("$n balasan", "$n replies")
    val chatThreadEmpty get() = t("Belum ada balasan. Jadilah yang pertama.", "No replies yet. Be the first.")
    val chatThreadHint get() = t("Balas di thread…", "Reply in thread…")
    val chatModTitle get() = t("Moderasi", "Moderation")
    val chatModDelete get() = t("Hapus (moderator)", "Delete (moderator)")
    val chatModWarn get() = t("Beri peringatan", "Warn user")
    val chatModMute get() = t("Mute sementara", "Temporary mute")
    val chatModUnmute get() = t("Buka mute di chat", "Lift chat mute")
    val chatModWarnReason get() = t("Alasan peringatan (opsional)", "Warning reason (optional)")
    val chatModWarnNote get() = t("3 peringatan otomatis membuat pengguna dimute 24 jam.", "3 warnings automatically mute the user for 24 hours.")
    val chatModMuteDuration get() = t("Durasi mute", "Mute duration")
    fun chatModHours(n: Int) = t("$n jam", "$n h")
    val chatModReports get() = t("Kelola laporan", "Manage reports")
    val chatModReportsEmpty get() = t("Tidak ada laporan", "No reports")
    val chatModDismiss get() = t("Abaikan", "Dismiss")
    fun chatModReportBy(by: String, reason: String) = t("Dilaporkan @$by · $reason", "Reported by @$by · $reason")
    fun chatModWarned(name: String, n: Int) = t("@$name diperingatkan ($n)", "@$name warned ($n)")
    fun chatModMuted(name: String, h: Int) = t("@$name dimute $h jam", "@$name muted for $h h")
    val chatModUnmuted get() = t("Mute dibuka", "Mute lifted")
    val chatModDeletedOk get() = t("Pesan dihapus", "Message deleted")
    val chatNotifChannelWarn get() = t("Peringatan moderator", "Moderator warnings")
    val chatNotifWarnTitle get() = t("Peringatan dari moderator", "Moderator warning")
    val chatBlockAction get() = t("Blokir pengguna", "Block user")
    val chatUnblockAction get() = t("Buka blokir", "Unblock")
    fun chatBlockedOk(name: String) = t("@$name diblokir. Pesannya disembunyikan.", "@$name blocked. Their messages are hidden.")
    fun chatUnblockedOk(name: String) = t("Blokir @$name dibuka", "@$name unblocked")
    val chatPrivacyTitle get() = t("Privasi & blokir", "Privacy & blocking")
    val chatPrivacyPresence get() = t("Sembunyikan status online", "Hide online status")
    val chatPrivacyPresenceSub get() = t("Kamu terlihat offline dan terakhir aktif disembunyikan", "You appear offline and your last active time is hidden")
    val chatPrivacyProfile get() = t("Sembunyikan profil", "Hide profile")
    val chatPrivacyProfileSub get() = t("Bio, status, dan statistik hanya terlihat olehmu", "Bio, status, and stats are only visible to you")
    val chatPrivacyMusic get() = t("Sembunyikan aktivitas musik", "Hide music activity")
    val chatPrivacyMusicSub get() = t("Lagu disukai, playlist, dan lagu sering diputar tidak terlihat orang lain", "Liked songs, playlists, and top tracks are hidden from others")
    val chatBlockedUsers get() = t("Pengguna diblokir", "Blocked users")
    val chatNoBlocked get() = t("Belum ada pengguna yang diblokir", "No blocked users")
    val chatProfileHiddenInfo get() = t("Profil disembunyikan oleh pemiliknya", "This profile is hidden by its owner")
    val chatMusicHiddenInfo get() = t("Aktivitas musik disembunyikan", "Music activity is hidden")
    fun chatYourWarns(n: Int) = t("Kamu punya $n peringatan moderator", "You have $n moderator warning(s)")
    val chatErrNewAccount get() = t("Akun baru dibatasi (tautan, mention, dan jumlah pesan). Coba lagi nanti.", "New accounts are limited (links, mentions, message count). Try again later.")
    val chatErrMentionSpam get() = t("Terlalu banyak mention. Maksimal 3 per pesan.", "Too many mentions. Maximum 3 per message.")
    val chatErrSpam get() = t("Pesan dianggap spam", "That message looks like spam")
    val chatErrForbidden get() = t("Kamu tidak punya izin untuk itu", "You don't have permission for that")
    val feedbackEntry get() = t("Saran & Bug", "Ideas & Bugs")
    val feedbackEntrySub get() = t("Kirim ide fitur dan laporkan bug", "Suggest features and report bugs")
    val feedbackTitle get() = t("Saran & Laporan Bug", "Ideas & Bug Reports")
    val feedbackTabFeatures get() = t("Ide fitur", "Feature ideas")
    val feedbackTabMine get() = t("Punyaku", "Mine")
    val feedbackNew get() = t("Kirim saran atau bug", "Send an idea or bug")
    val feedbackKindFeature get() = t("Ide fitur", "Feature idea")
    val feedbackKindBug get() = t("Laporan bug", "Bug report")
    val feedbackTitleLabel get() = t("Judul singkat", "Short title")
    val feedbackBodyFeature get() = t("Ceritakan idemu dan manfaatnya", "Describe your idea and why it helps")
    val feedbackBodyBug get() = t("Apa yang terjadi dan langkah untuk memunculkannya", "What happened and the steps to reproduce it")
    val feedbackSubmit get() = t("Kirim", "Send")
    val feedbackSent get() = t("Terkirim. Terima kasih!", "Sent. Thank you!")
    val feedbackEmpty get() = t("Belum ada ide fitur. Kirim yang pertama!", "No feature ideas yet. Send the first one!")
    val feedbackMineEmpty get() = t("Kamu belum mengirim apa pun", "You haven't sent anything yet")
    fun feedbackVotes(n: Int) = t("$n suara", "$n votes")
    val feedbackVote get() = t("Dukung", "Upvote")
    val feedbackVoted get() = t("Didukung", "Upvoted")
    val feedbackStatusOpen get() = t("Baru", "New")
    val feedbackStatusPlanned get() = t("Direncanakan", "Planned")
    val feedbackStatusProgress get() = t("Dikerjakan", "In progress")
    val feedbackStatusDone get() = t("Selesai", "Done")
    val feedbackStatusRejected get() = t("Ditolak", "Declined")
    val feedbackOwnerNote get() = t("Catatan owner", "Owner's note")
    val feedbackDelete get() = t("Hapus", "Delete")
    val feedbackDeviceNote get() = t("Info perangkat dan versi aplikasi ikut dikirim untuk laporan bug.", "Device and app version are included with bug reports.")
    val feedbackBugPrivacy get() = t("Laporan bug hanya terlihat olehmu dan owner.", "Bug reports are only visible to you and the owner.")
    val feedbackDeleted get() = t("Dihapus", "Deleted")

}

val LocalStrings = staticCompositionLocalOf { Strings(false) }

/** Pemakaian di dalam composable: `S.settings`. Ikut recompose saat bahasa berganti. */
val S: Strings
    @Composable
    @ReadOnlyComposable
    get() = LocalStrings.current

/** Untuk kode non composable (event handler, ViewModel, service, widget). Selalu baca bahasa terbaru. */
fun currentStrings(): Strings = Strings(SettingsManager.getInstance().isEnglish())
