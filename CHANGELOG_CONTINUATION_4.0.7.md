# Nexrify 4.0.7 — continuation patch

This patch keeps `versionName=4.0.7` and `versionCode=38`.

## Implemented in this patch
- Song comments are presented in a full-screen page/dialog instead of a bottom sheet.
- Long-pressing a comment opens actions to reply; owners can delete their own comments.
- Comment editing is not exposed in the UI, and the edit action has been removed from the API.
- The send button is locked while a comment request is in progress.
- Comment sends include a request ID; the API stores the result for 24 hours so a repeated request ID returns the original comment instead of creating another one.

## Not yet implemented / verified
- Staff/Admin theme entitlement is now supported through server-side allowlists; deployment must configure the trusted usernames in the API environment.
- Spotify playlist import is not implemented in this patch. It requires configured Spotify credentials and a verified catalogue-matching provider.
- Translated lyrics, media uploads, and remaining visual refinements still require implementation and device testing. Search playback remains on its current navigation destination because the full player is an overlay, not a navigation route.
- Android compilation could not be run because the Gradle 8.7 distribution could not be downloaded in this environment.

No APK is claimed to be build-verified.

## Lanjutan pekerjaan
- Pull-to-refresh pada Beranda dan halaman hasil pencarian; Beranda memaksa penyegaran cache rak musik.
- Penyegaran Beranda sekarang selalu menutup status loading walaupun permintaan gagal.
- Hak tema eksklusif sekarang menerima Top 3 Supporter serta role staff/admin yang dikirim server dari allowlist environment. Role tidak dapat ditentukan oleh aplikasi klien.
- Pemetaan judul rak menambahkan terjemahan untuk Video Musik Terbaru dan Mix Lagu.
- Import playlist dan terjemahan lirik lanjutan sengaja belum ditambahkan pada tahap ini karena memerlukan integrasi/provider dan sumber terjemahan yang dapat diverifikasi.
