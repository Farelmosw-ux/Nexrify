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
