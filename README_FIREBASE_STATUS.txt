MediaMatch update:
- Profil client dibuat mengikuti tampilan Figma: header biru, logo M, card profil, aktivitas, pengaturan, logout.
- Edit profil dibuat mengikuti Figma: foto profil bulat, pilih foto dari galeri, nama, email, nomor WhatsApp, lokasi, simpan perubahan.
- Navbar Chat disamakan dengan navbar Home/Order/Profile.
- Status chat online/offline memakai Firebase Realtime Database path: status/{uid}.

Penting:
Agar status online/offline bekerja otomatis saat aplikasi ditutup, aktifkan Firebase Realtime Database di Firebase Console.
Jika belum aktif, chat tetap berjalan tetapi status online/offline bisa tidak tampil realtime.
