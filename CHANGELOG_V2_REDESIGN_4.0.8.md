# 4.0.8: rombak tema Liquid Glass V2 dan pencarian Koleksi

## Pencarian (semua tema)
- Lagu disukai: kotak cari di atas daftar, cocok ke judul dan artis (urutan kata bebas).
- Playlist: cari nama playlist atau lagu di dalamnya. Di dalam satu playlist ada kotak cari lagu sendiri.
- Offline: cari di lagu yang sudah diunduh. Saat mencari, tombol Hapus semua disembunyikan supaya tidak kepencet.
- Ada tampilan "tidak ada hasil" dan tombol hapus teks.

## Liquid Glass V2 (hanya tema ini yang berubah)
- Warna baru "Aurora Violet": latar ungu malam, aksen violet dan teal (mode terang juga ada).
- Navigasi pindah ke atas layar (tab aktif melebar dan menampilkan label), mini player tetap di bawah.
- Beranda: tombol aksi di atas, sapaan di bawahnya dalam satu kartu kaca. Urutan rak dibalik (Pilihan untukmu, Favorit, lalu Putar lagi). Donasi dan Masukan pindah dari atas ke bawah, jadi dua kartu bersebelahan.
- Koleksi: urutan jadi pencarian, tab, lalu kepala daftar (tema lain: kepala, tab, pencarian).
- Daftar lagu, kartu lagu, dan mini player: nama artis di atas judul. Bilah progres mini player pindah ke bawah.
- Pemutar penuh: artwork jadi kartu membulat, urutan kontrol dibalik (pintasan, tombol putar, lalu bilah waktu). Tombol putar memakai gradasi aksen.

## Catatan
- Belum dikompilasi di lingkungan ini (tidak ada akses ke Gradle). Jalankan build lewat GitHub Actions seperti biasa dan kabari kalau ada error.

## Role Developer dan efek kilau
- Role baru `developer` (server dan app). Atur lewat panel admin (Peran chat) atau env `CHAT_DEVELOPER=username1,username2`.
- Foto profil dan lencana role sekarang berkilau: Admin emas, Staff perak, Developer RGB (emas, merah, oranye, hijau, biru, ungu).
- Di sheet profil muncul lencana role di bawah @username. Lencana kecil yang sama dipakai di chat.
- Developer punya hak seperti admin (kelola grup, tema premium) dan hanya developer yang bisa menindak developer.

## Revisi V2 (feedback)
- Warna V2 dikembalikan ke tampilan semula (kaca abu gelap). Navigasi utama (Beranda, Cari, Koleksi) kembali di bawah.
- Beranda V2 dapat bar sendiri di atas: profil, chat, surat, dan pengaturan. Sapaan jadi kartu terpisah di bawahnya.
- Foto profil bisa dibuka layar penuh (ketuk fotonya di sheet profil, bisa dijepit untuk zoom). Jendela ini aman dari screenshot dan rekam layar (FLAG_SECURE). Di Android 14 ke atas, percobaan screenshot memunculkan pesan dari app.

## Revisi 3
- Mini player yang ditutup (panah bawah) sekarang punya tombol panah atas kecil di pojok kanan bawah buat memunculkannya lagi. Ada titik kecil kalau lagu sedang diputar.
- Tag Top Donate berkilau berjalan: Top 1 emas, Top 2 perak, Top 3 perunggu. Banner donatur peringkat 2 dan 3 juga ikut berkilau.
- Foto profil layar penuh sekarang kotak selebar layar di tengah (gaya WhatsApp), dengan tombol kembali dan nama di atas. Tetap diproteksi dari screenshot.
