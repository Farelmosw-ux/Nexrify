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
