# Nexrify

Aplikasi streaming musik Android (Jetpack Compose + Media3).

- Package: `id.nexrify.music`
- Developer: Nexra Destroyer
- Versi: 4.0.0

## Fitur
- Background playback: notifikasi, lock screen, headset/Bluetooth, widget layar utama
- Smart Queue: tambah, putar berikutnya, drag-and-drop, hapus, simpan jadi playlist
- Home personal: lanjutkan pemutaran, terakhir diputar, favorit, playlist, rekomendasi
- Animasi: loading screen, skeleton, mini player -> Now Playing, transisi fade + slide

Semua data (playlist, favorit, antrean, riwayat) hanya tersimpan di perangkat. Tanpa login, tanpa server.

## Membuat keystore sendiri (Termux)

```
pkg install openjdk-17
keytool -genkeypair -v -keystore nexrify.keystore -alias nexrify \
  -keyalg RSA -keysize 2048 -validity 10000
```
Isi password (catat!) dan data diri. Simpan file `nexrify.keystore` di tempat aman, JANGAN di-commit.

### Build lokal
Buat `keystore.properties` di root proyek (sudah di-ignore git):
```
storeFile=/path/ke/nexrify.keystore
storePassword=PASSWORD_ANDA
keyAlias=nexrify
keyPassword=PASSWORD_ANDA
```

### Build lewat GitHub Actions
```
base64 -w0 nexrify.keystore > nexrify.b64
```
Di repo: Settings -> Secrets and variables -> Actions -> New repository secret:

| Nama | Isi |
|---|---|
| NEXRIFY_KEYSTORE_BASE64 | isi file nexrify.b64 |
| NEXRIFY_STORE_PASSWORD | password keystore |
| NEXRIFY_KEY_ALIAS | nexrify |
| NEXRIFY_KEY_PASSWORD | password key |

Tanpa secrets, APK release tetap jadi tetapi ditandatangani kunci debug.

## Rilis
```
git tag v4.0.0
git push origin v4.0.0
```
APK release otomatis muncul di tab Releases.
