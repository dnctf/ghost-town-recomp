# Native-feel UX — rencana, belum diimplementasikan

Status: desain UX dan kontrak fitur. Tidak ada perubahan pada jalur runtime, rendering, input atau Play gate.

## Alur Play tanpa desktop Wine

1. Pengguna memilih game dari Library; aplikasi memeriksa instalasi game tanpa mengunggah konten.
2. Runtime Manager memeriksa komponen yang dibutuhkan, versi, ABI dan SHA-256; komponen hilang/rusak diunduh ke staging dan diverifikasi.
3. Installer mengaktifkan komponen secara atomik, membuat/mengembalikan prefix Wine tervalidasi, dan menyiapkan direktori save/cache.
4. Self-test memeriksa executable Box64, Wine/prefix, driver GPU, renderer, input backend dan audio backend. Kegagalan tampil sebagai pesan actionable dan diagnostic ID.
5. Display self-test membuka surface fullscreen, memvalidasi frame nyata dan renderer. Hanya hasil sukses yang membuat GHOST_TOWN_DISPLAY_READY=1.
6. Play meluncurkan executable game secara langsung dalam mode fullscreen; desktop Wine bukan bagian dari alur pengguna normal. Desktop/debug shell hanya developer option.
7. Saat keluar/crash, aplikasi menyimpan exit status, waktu, log tersanitasi dan menawarkan recovery tanpa mereset save.

## Kontrol dan co-op

- Gamepad Bluetooth dideteksi melalui Android InputDevice API; tampilkan nama/perangkat dan status koneksi.
- Pemetaan otomatis per device menggunakan profil XInput yang eksplisit; sediakan remap manual, deadzone dan kalibrasi.
- Multi-gamepad lokal: setiap controller memiliki player slot terpisah; tangani disconnect/reconnect dan duplicate device IDs.
- Touch overlay dapat dipilih per game dan tidak boleh mengambil alih input ketika controller fisik aktif, kecuali pengguna memilih kombinasi.
- Co-op Overcooked! 2 menjadi prioritas uji. Dukungan jumlah pemain dan split mapping wajib diverifikasi di perangkat; contoh profile 4 pemain saat ini hanya rancangan.

## Overlay dan performa

- Overlay toggleable: FPS (current/average/1% low jika metrik valid), suhu perangkat/baterai jika API OS mengizinkan, RAM proses/app, resolusi internal, render scale dan nama driver.
- Jangan mengklaim suhu GPU bila Android API hanya memberi suhu baterai/skin. Tampilkan jenis sensor dan status unavailable.
- Render scale dapat diubah per game dengan pilihan konservatif dan perubahan berlaku saat restart jika backend tidak mendukung resize dinamis.
- Preset thermal: cool/balanced/performance; FPS cap dan resolusi diturunkan bertahap berdasarkan kebijakan yang diuji, bukan heuristik yang belum terukur.
- Catat frame pacing dan thermal throttling secara terpisah dari FPS rata-rata.

## Cache shader

- Cache dipisah per game ID + versi driver GPU + DXVK/Mesa + pipeline cache UUID.
- Simpan di app-private storage, gunakan batas ukuran/LRU, dan hapus hanya cache yang terkait saat driver/runtime berubah.
- Cache corrupt harus dapat dihapus dan dibangun ulang. Jangan mencampur cache antar game atau antar driver.
- Shader compilation stutter harus dicatat; jangan mendistribusikan shader cache yang berasal dari game pengguna.

## Save data backup/restore

- Identifikasi prefix dan lokasi save per game; tampilkan lokasi sumber dan ukuran sebelum backup.
- Backup menjadi arsip app-private atau file yang dipilih pengguna melalui Storage Access Framework.
- Sertakan metadata game/profile/version dan checksum; jangan unggah ke CI/cloud tanpa persetujuan eksplisit.
- Restore harus preview, membuat backup otomatis sebelum menimpa, validasi path traversal, lalu atomic replace.
- Jangan menghapus save saat reinstall/upgrade runtime. Uji backup/restore pada salinan save dummy sebelum game asli.

## Gerbang implementasi

- Semua fitur menunggu Gerbang P runtime pack dan Gerbang D display yang lulus.
- Input, audio, shader cache dan save backup diuji terpisah sebelum integrasi game.
- Marker display tidak boleh dibuat dari UI preview, simulasi, atau status konfigurasi.
- Belum ada klaim dukungan gamepad multi-device, overlay sensor, shader cache, atau save restore di MatePad.
