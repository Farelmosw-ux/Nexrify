# Nexrify Web (nexrify.biz.id)

Situs resmi Nexrify: landing page, download, catatan rilis, bantuan, privasi, syarat, dan panel admin di **/nexrapanel**.
Bahasa Indonesia dan English, warna dan font sama dengan aplikasi. Hosting di Vercel, kode di GitHub, tanpa dependensi npm.

## Isi

| Jalur | Fungsi |
|---|---|
| `/`, `/download`, `/changelog`, `/help`, `/privacy`, `/terms` | Halaman publik (ID/EN, tema gelap/terang) |
| `/nexrapanel` | Panel admin: rilis (unggah APK), kotak surat, kontak, status sistem |
| `GET /api/release` | **Detect release** untuk aplikasi dan website (`?all=1` = semua versi) |
| `GET /api/download[?v=3.8.1]` | Hitung unduhan lalu arahkan ke APK (tanpa `v` = terbaru) |
| `GET /api/mailbox` | Surat untuk kotak surat di aplikasi |
| `GET /api/site` | Kontak publik untuk footer |
| `POST /api/auth` | Akun Nexrify: `register`, `login`, `me`, `update` (nama dan foto), `password`, `logout`, `delete` |
| `GET/PUT /api/user` | Data akun (favorit, playlist, riwayat, pengaturan) untuk sinkron antar perangkat. Wajib token |
| `POST /api/admin` | Semua aksi panel (login wajib) |

Contoh respons `/api/release`:

```json
{ "ok": true, "release": { "version": "3.9.0", "versionCode": 32, "notes": { "id": "...", "en": "..." },
  "size": 31457280, "publishedAt": 1793000000000, "mandatory": false,
  "downloadUrl": "https://nexrify.biz.id/api/download?v=3.9.0", "minAndroid": "8.0" } }
```

## Pasang di Vercel

1. **Buat repo GitHub** untuk kode ini (isi folder ini) lalu **Import** ke Vercel. Framework: *Other*. Tidak ada build command.
2. **Repo file APK**: buat repo GitHub **Public** terpisah (misalnya `nexrify-files`). Buat token *fine-grained* dengan izin **Contents: Read and write** hanya untuk repo itu.
3. **Upstash**: buat database Redis gratis di upstash.com, salin *REST URL* dan *REST Token* (tab REST API).
4. **Environment Variables** di Vercel (lihat `.env.example`): `ADMIN_USER`, `ADMIN_PASS`, `ADMIN_SECRET`, `UPSTASH_REDIS_REST_URL`, `UPSTASH_REDIS_REST_TOKEN`, `GITHUB_TOKEN`, `GITHUB_USR`, `GITHUB_REPO`. Opsional: `SITE_URL`, `CONTACT_*`. Redeploy setelah mengisi.
5. **Domain**: Vercel, Settings, Domains, tambahkan `nexrify.biz.id` (dan `www.nexrify.biz.id` dialihkan ke domain utama). Di DNS domain: record **A** `@` ke `76.76.21.21` dan **CNAME** `www` ke `cname.vercel-dns.com` (atau ikuti nilai yang Vercel tampilkan). HTTPS otomatis.

## Alur rilis aplikasi

1. Build APK lewat GitHub Actions di repo aplikasi (workflow yang sudah ada).
2. Buka `https://nexrify.biz.id/nexrapanel`, login, tab **Rilis**, **+ Rilis baru**.
3. Isi **versi** dan **version code** persis seperti di `app/build.gradle.kts` (`versionName` dan `versionCode`), catatan rilis ID dan EN, pilih APK, lalu **Unggah dan simpan**.
4. Aplikasi yang sudah terpasang mengecek `/api/release` saat dibuka. Kalau versi di situs lebih baru, muncul dialog **Update tersedia** dengan tombol **Nanti** dan **Update sekarang**. Centang *Update wajib* untuk menghilangkan tombol Nanti.

Rilis terbaru otomatis = version code tertinggi yang berstatus terbit. Tombol **Jadikan terbaru** mengunci versi tertentu (berguna untuk rollback).

## Kotak surat

Tab **Kotak surat** di panel: buat surat dengan jenis (Info, Update, Promo, Penting), judul dan isi dua bahasa, tautan opsional, tanggal berakhir, dan sematkan. Surat tayang di ikon amplop di Beranda aplikasi.

## Keamanan

- Panel di `/nexrapanel` (bukan `/admin` atau `/panel`), tidak diindeks mesin pencari, sesi cookie HttpOnly bertanda tangan HMAC, batas 5 percobaan login per 10 menit.
- Token GitHub hanya dikirim ke browser admin yang sudah login, dipakai langsung untuk unggah APK (melewati batas ukuran request Vercel).
- Halaman Privasi dan Syarat adalah **templat awal**. Tinjau sendiri, dan sesuaikan dengan kondisi hukum dan layananmu sebelum dipublikasikan.

## Pengembangan lokal

`python3 build_pages.py` membuat ulang halaman statis di `public/` dari templat di file itu. Panel dan JS ada langsung di `public/`.

## Akun pengguna (login wajib di aplikasi)

Aplikasi mewajibkan masuk atau daftar sebelum dipakai. Akun dibuat langsung di server ini, tanpa Firebase:

- Daftar dengan **username** (3 sampai 20 karakter huruf kecil, angka, garis bawah), **password** (6 sampai 72), dan nama tampilan. Password disimpan sebagai hash scrypt dengan garam acak.
- Sesi berupa token bertanda tangan HMAC (berlaku 60 hari) memakai `ADMIN_SECRET`. Ganti password, blokir, atau reset dari panel langsung mencabut semua sesi.
- Foto profil: JPEG 256 px yang dikecilkan di aplikasi (disimpan sebagai data URL, maksimal sekitar 100 KB).
- Pembatasan: 8 pendaftaran per jam dan 15 percobaan masuk per 10 menit per alamat IP.
- **Sinkron data**: aplikasi memantau penyimpanan lokal dan mengirim perubahan ke `/api/user` setelah jeda singkat. Batas data per akun sekitar 900 KB (batas request Upstash gratis 1 MB).
- **Kotak surat**: tab Kotak surat di panel bisa mengirim ke semua pengguna atau ke satu username. Aplikasi mengecek tiap 10 menit saat prosesnya hidup dan menampilkan notifikasi untuk surat baru. Notifikasi saat aplikasi benar benar mati butuh Firebase Cloud Messaging (belum dipasang).
- Tab **Pengguna** di panel: lihat akun, kirim pesan ke satu pengguna, reset password, blokir, dan hapus akun beserta datanya.

Catatan: `ADMIN_SECRET` wajib diisi dan jangan diganti sembarangan, karena semua token pengguna ikut tidak berlaku kalau diganti.

## Global Chat (v4.0.6)

Ruang obrolan publik untuk semua pengguna Nexrify. Tanpa layanan tambahan: memakai Vercel dan Upstash Redis yang sudah ada.

- `api/chat.js` : endpoint pengguna (kirim, edit, hapus, reaksi, mengetik, laporan, profil, cari pengguna, notifikasi, foto profil)
- `api/_chat.js` : inti chat dan fungsi moderasi untuk panel
- `api/_chatfilter.js` : sensor kata kasar (babi → ####)
- Panel `/nexrapanel` → tab **Obrolan**: kirim pengumuman, lihat laporan, hapus pesan, batasi pengguna
- Environment opsional: `CHAT_STAFF=username1,username2` memberi lencana Staf
- Hanya teks dan emoji. Tidak ada unggah foto, video, atau file.
- Real-time memakai polling 3 detik yang melambat saat sepi dan berhenti saat layar chat ditutup, supaya muat di kuota gratis Upstash.

**Aturan produk:** Nexrify hanya punya Global Chat (publik) dan profil pengguna. Tidak ada pesan langsung, chat pribadi, atau grup privat, dan tidak akan ditambahkan.

### Info grup, anggota, dan status online
- Info grup (nama, deskripsi, foto) disimpan di Redis (`nx:chat:group`) dan hanya bisa diubah oleh Admin dan Staf.
- Peran: env `CHAT_ADMIN` / `CHAT_STAFF` (username dipisah koma) atau lewat panel tab Obrolan.
- Anggota = pengguna yang sudah membuka Global Chat atau aplikasi versi 4.0.6 ke atas.
- ONLINE = ada aktivitas ke server dalam 150 detik terakhir; selain itu OFFLINE.

## Donasi / Dukung Developer
Sukarela, lewat **QRIS statis** dan **DANA**. Donatur wajib mengunggah foto bukti; bukti + username akun otomatis dikirim ke Telegram owner dengan tombol ✅ Setujui / ❌ Tolak.

1. Buat bot di @BotFather, isi `TELEGRAM_BOT_TOKEN`, `TELEGRAM_OWNER_CHAT_ID`, `TELEGRAM_WEBHOOK_SECRET` (huruf/angka/`_`/`-`, min 20 karakter), lalu **Redeploy**.
2. Buka sekali di browser: `https://DOMAIN/api/telegram?key=ISI_TELEGRAM_WEBHOOK_SECRET` (menambahkan `&action=info` untuk cek status).
3. Isi `DANA_ACCOUNT_NAME` / `DANA_ACCOUNT_NUMBER` (atau lewat panel `/nexrapanel` tab **Donasi**).
4. Gambar QRIS diambil dari `QRIS_PAGE_URL` (halaman ibb) lalu di-cache 12 jam. Kalau gagal, isi `QRIS_IMAGE_URL` dengan link gambar langsung.

Endpoint: `GET/POST /api/donate`, `GET/POST /api/telegram`. Peringkat 3 teratas dihitung dari total donasi yang disetujui; panel bisa menambah, mengubah, dan menghapus donatur secara manual.
