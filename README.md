# Nexrify

Aplikasi streaming musik Android (Jetpack Compose + Media3).

- Package: `id.nexrify.music`
- Developer: Nexra Destroyer
- Tema warna: tidak berubah dari versi sebelumnya (hitam pekat / putih)

## Fitur v4.0.0

**Pemutaran latar belakang**
- Kontrol dari notifikasi, lock screen, headset kabel/Bluetooth
- Musik tetap jalan saat aplikasi diminimalkan atau di-swipe dari recent apps
- Widget layar utama (cover, judul, prev / play-pause / next)
- Kapsul musik (Now Bar / Dynamic Island, dll.) muncul otomatis di perangkat yang mendukung, bersumber dari MediaSession

**Smart Queue**: tambah ke antrean, putar berikutnya, drag-and-drop, hapus, simpan sebagai playlist

**Home personal**: lanjutkan pemutaran terakhir, terakhir diputar, lagu favorit, playlist pengguna, rekomendasi dari kebiasaan mendengarkan

**Animasi**: loading screen, skeleton berkilau, error + coba lagi, mini player -> Now Playing, transisi fade + slide, animasi favorit & playlist

## Penyimpanan data

Tanpa login. Playlist, favorit, riwayat, antrean, dan sesi terakhir disimpan **hanya di perangkat ini** (SharedPreferences, `allowBackup=false`). Tidak ada sinkronisasi dan tidak ada data global.

## Build

```
./gradlew assembleDebug
./gradlew assembleRelease
```
