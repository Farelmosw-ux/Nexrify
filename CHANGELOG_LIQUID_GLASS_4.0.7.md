# Nexrify v4.0.7 — Liquid Glass continuation

## Perubahan
- Normalisasi judul section Home agar variasi kapitalisasi dari API tetap diterjemahkan ke English (`Quick picks`, `Latest Music Videos`, `Music Mixes`, dan judul terkait).
- Mengganti daftar tema lama menjadi `Classic` dan `Liquid Glass` baru; label tema eksklusif lama dihilangkan dari UI.
- Liquid Glass baru dibatasi untuk akun Top 3 Donatur. Akun `staff` dan `admin` mendapat akses langsung tanpa ranking.
- Tema lama `Violet Night`, `Aurora Gold`, dan `Obsidian` dimigrasikan otomatis ke Liquid Glass baru.
- Navbar Liquid Glass memakai panel transparan, indikator pilihan yang bergerak, dan gesture geser horizontal untuk berpindah tab.
- Mini player mendapat permukaan kaca dan highlight lensa.
- Switch pengaturan mendapat backing kaca; slider pemutar memakai aksen Liquid Glass.
- Efek blur dekoratif hanya diaktifkan pada Android 13+ (API 33). Android lebih lama menggunakan permukaan transparan dan border ringan.
- Auth API sekarang menyertakan role dari sumber server (`C.roleOf`) pada respons akun agar akses staff/admin tidak bergantung pada klaim klien.

## Verifikasi
- `node --check api/auth.js` lulus.
- `node --check api/song-comments.js` lulus.
- Build Kotlin belum dapat diverifikasi karena Gradle 8.7 harus diunduh dari `services.gradle.org`, sedangkan koneksi DNS lingkungan ini gagal.
