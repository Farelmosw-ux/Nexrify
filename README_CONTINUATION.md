# Nexrify continuation notes

This archive continues the native Android/Kotlin project. Existing package ID, UI palette, and signing setup are retained.

## Implemented in this continuation
- Added a reusable local-time context resolver with deterministic boundaries for Early Morning, Before Sunrise, Morning, Late Morning, Afternoon, Late Afternoon, Evening, Night, Late Night, Midnight, and Midday.
- Home greeting uses the resolver and selects Indonesian or English copy based on the persisted language preference.
- Listening statistics are exposed as observable StateFlows and the statistics dialog collects them, so totals and artist counts can update while the dialog is open.
- Sync YT now exposes a user-facing status for login-required, syncing, successful results, empty/unavailable results, and failures rather than silently returning.
- Removed the duplicated nested copy of the project from the ZIP; the root project is the single canonical project.

## Still requiring external/device integration and validation
- Magrib greeting now uses a timezone-only approximate window: WIB 18:00, WITA 18:10, WIT 18:20, shown for 20 minutes. This is not an exact prayer timetable; Magrib varies by city and date. It does not use GPS or claim location-specific accuracy.
- Full localization of every screen is not complete.
- Google profile name/avatar depends on the actual sign-in provider and profile data returned by it; current project authentication is YouTube web-cookie based, not a complete Google Sign-In SDK integration.
- GitHub Releases check assumes a release exists at the configured repository and requires internet access.
- The playback service is declared exported so Android System UI and permitted external media controllers can discover/connect to the Media3 MediaSessionService. The service preserves the media session when the app task is swiped away during active playback, restores widget track state from the live media session, and loads track artwork asynchronously for the home-screen widget. Media3 supplies the playback notification and lock-screen controls from the same session. OEM music capsules / Dynamic Island-like status-bar panels are controlled by the device OS; this configuration exposes the standard Android media session that such features commonly use, but cannot force the capsule to appear on every device. Verify on the physical device and enable media notifications / music capsule in system settings if needed.
- Sync YT depends on YouTube Music's current authenticated endpoints and can fail when the upstream service changes.
- Full Gradle build was attempted but cannot run in this environment because `services.gradle.org` cannot be resolved. No APK build success is claimed.


## Pembaruan 3.5.0
- Panel musik kapsul (capsule/MusicCapsuleController.kt): overlay di atas layar saat lagu diputar dan Nexrify tidak dibuka. Ketuk 1x membuka aplikasi, tekan lama membuka panel penuh (judul, artis, seekbar, prev/play/next). Butuh izin "tampil di atas aplikasi lain", diatur dari Pengaturan > Panel musik kapsul.
- Halaman pencarian sendiri (SearchPageScreen) dengan riwayat pencarian yang bisa dihapus satu per satu atau semuanya (SearchHistoryManager).
- Ikon bergaya Iconsax di seluruh aplikasi (ui/icons/NexIcons.kt, digambar ulang sebagai vector Compose).
- Bahasa realtime Indonesia dan Inggris: semua teks ada di i18n/Strings.kt dan diamati lewat LocalStrings, jadi ganti bahasa langsung berubah tanpa restart.
- Loading baru (NexLoading): lingkaran berputar, progress bar, dan angka persen. Layar awal memakai progres asli dari pemuatan beranda.
- Footer baru (NexFooter) di beranda dan Pengaturan, versi dibaca dari paket, bukan teks tetap.
- Belum bisa dikompilasi di lingkungan pembuatan (tidak ada akses jaringan untuk Gradle). Cek build lewat GitHub Actions atau Android Studio.

## Revisi 3.5.1
- Notifikasi musik anti terhapus: PersistentNotificationProvider menandai notifikasi ongoing dan no clear selama lagu diputar.
- Kapsul tidak hilang saat memutar atau ganti lagu: ada jeda aman 4 detik, judul dan sampul terakhir dipertahankan, sampul punya cache, retry, dan placeholder.
- Maghrib dihitung dari lokasi kasar pengguna (data/prayer/MaghribManager.kt), zona waktu mengikuti perangkat. Tanpa lokasi, tidak ada tebakan, sapaan Maghrib tidak tampil dan Pengaturan menjelaskan statusnya.
- Waktu sapaan lain dibuat relatif terhadap Maghrib agar tidak bertabrakan.
- Fitur Sync YT dihapus.
- Nama dan foto profil diambil dari akun Google/YouTube dan disegarkan tiap aplikasi dibuka.

## Revisi 3.5.2: lanjut dari terakhir
- data/playback/PlaybackStateStore.kt menyimpan antrian (jendela sekitar lagu aktif), lagu, posisi, mode acak dan ulang. Disimpan tiap pergantian lagu, jeda atau putar, dan tiap 5 detik saat memutar.
- MusicViewModel memulihkan state saat dibuat. Lagu tidak langsung bunyi: mini player menampilkan lagu terakhir dalam keadaan jeda, dan saat tombol play atau seekbar ditekan lagu dimuat ulang (stream diambil ulang karena kedaluwarsa) lalu lanjut dari posisi tersimpan.
- MusicViewModel kini dipegang di level aplikasi (NexrifyApplication sebagai ViewModelStoreOwner), sehingga tombol next dan prev di notifikasi dan kapsul tetap jalan walau Activity sudah ditutup selama proses masih hidup.

## Revisi 3.6.0: antrian lagu
- Perbaikan build: MaghribManager tidak lagi memakai LocationManagerCompat.getCurrentLocation (overload ambigu), diganti requestSingleUpdate dengan LocationListener dan batas tunggu 30 detik.
- QueueSheet (ui/components/QueueSheet.kt), dibuka dari tombol antrian di pemutar penuh: ketuk untuk loncat, naikkan atau turunkan, hapus, bersihkan lagu berikutnya. Lagu aktif tidak bisa dihapus.
- Menu titik tiga di daftar lagu (AddToPlaylistDialog) punya "Putar berikutnya" dan "Tambah ke antrian".
- MusicViewModel: queue, queueIndex, playFromQueue, removeFromQueue, moveInQueue, clearUpcoming, playNext, addToQueue. Perubahan antrian ikut tersimpan untuk fitur lanjut dari terakhir.

## Revisi 3.7.0: tombol suka di notifikasi dan kapsul
- service/PlaybackBridge.kt: jembatan satu proses antara MusicViewModel dan layanan (status suka lagu aktif dan aksi toggle).
- Notifikasi dan kontrol media sistem punya tombol hati (custom layout MediaSession, perintah ACTION_TOGGLE_LIKE) yang ikonnya berubah isi atau garis sesuai status.
- Panel penuh kapsul punya tombol hati di sisi kiri kontrol. Keduanya memakai MusicViewModel.toggleFavorite, jadi favorit tersimpan dan sinkron dengan layar aplikasi.

## Revisi 3.8.0: unduh offline dan perbaikan sampul kapsul
- Perbaikan sampul kapsul: tiap kapsul dipasang ulang (misalnya setelah keluar dari aplikasi), view barunya kosong sementara key sampul tidak berubah sehingga tidak dimuat lagi. Sekarang sampul terakhir dipasang ulang saat view dibuat.
- data/download/OfflineManager.kt: unduhan disimpan di filesDir/offline/<hash akun>/ (privat aplikasi, tidak masuk galeri atau file manager), daftar dan file terikat ke akun yang login, tanpa login memakai folder "guest". Unduh bertahap pakai header Range, antrean satu per satu, sampul ikut disimpan.
- service/OfflineDownloadService.kt: foreground service dataSync dengan notifikasi progres. Izin FOREGROUND_SERVICE_DATA_SYNC ditambahkan.
- MusicViewModel.playTrack memutar dari file lokal kalau lagu sudah diunduh (tanpa internet), termasuk sampul lokal untuk notifikasi dan kapsul.
- Koleksi punya tab Offline (OfflineTab.kt): putar semua, acak, hapus satu atau semua, unduhan berjalan dengan progres, batalkan, coba lagi. Menu titik tiga lagu punya tombol Unduh offline, Mengunduh, atau Hapus unduhan sesuai status.
- Logout tidak menghapus file, hanya menyembunyikannya sampai akun yang sama masuk lagi.

## Revisi 3.8.1: perbaikan putar offline dan kapsul
- Penyebab lagu offline tidak mau diputar: DefaultMediaSourceFactory hanya diberi DefaultHttpDataSource.Factory sehingga URI file:// ditolak. Sekarang memakai DefaultDataSource.Factory (http, file, content).
- Setelah error pemutaran, ViewModel tidak lagi loncat terus ke lagu streaming: lagu offline yang error tidak diloncati (streaming ulang kalau ada internet, berhenti kalau tidak ada), gagal beruntun 4 kali berhenti, tanpa internet next dan prev hanya memilih lagu yang sudah diunduh, radio otomatis tidak jalan saat offline, memilih lagu yang belum diunduh saat offline menampilkan pesan.
- data/network/NetworkMonitor.kt memantau koneksi, dan MainActivity menampilkan banner offline yang membuka tab Offline.
- Kapsul: hanya tampil selama musik diputar atau sedang menyiapkan lagu (PlaybackBridge.isLoading). Musik dijeda atau dimatikan: kapsul hilang otomatis setelah jeda 2 detik, kecuali panel penuh yang sedang dibuka.

## Revisi 3.9.0
- Kapsul: penyebab kapsul nyangkut ketemu. Tampilan baru belum menempel ke window beberapa frame pertama, pengecekan lama menganggapnya hilang lalu membuang referensinya tanpa melepasnya dari WindowManager, sehingga tampilan yatim tertinggal (muncul di dalam aplikasi, tetap ada saat musik mati, sampul lama). Sekarang semua tampilan dicatat dan dilepas bersama, dengan batas waktu sebelum dianggap hilang.
- Kapsul: tombol kunci (ketukan tidak membuka aplikasi, teks sesuai bahasa), tombol sembunyikan kapsul, dan tombol kapsul di kontrol media (di samping tombol suka) untuk menampilkannya lagi.
- Beranda baru: Putar lagi (grid 2 kolom), Pilihan untukmu (banner), Favorit, lalu rak lain. Tombol kotak surat dengan badge.
- Update: dialog Update tersedia (catatan rilis sesuai bahasa, tombol Nanti dan Update sekarang) lewat https://nexrify.biz.id/api/release. Cek di Pengaturan memakai endpoint yang sama.
- Kotak surat dari https://nexrify.biz.id/api/mailbox (cache offline, tandai dibaca, hapus lokal).
- Domain dipusatkan di data/remote/NexrifyApi.kt (NexrifyLinks). Website ada di nexrify-web.zip.
- Admin web: /nexrapanel. versionCode dan versionName di build.gradle harus sama dengan yang diisi saat rilis di panel.
