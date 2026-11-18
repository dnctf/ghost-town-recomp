# Strategi arsitektur: pengalaman native tanpa port native

Status: studi awal; belum ada pilihan final. "Native feel" berarti launcher Android, layar penuh, lifecycle, kontrol, diagnostik, dan pengelolaan runtime terasa seperti aplikasi Android. Eksekusi game tetap Windows x86_64 melalui translasi/emulasi; tanpa source Unity, ini bukan port native Unity.

## Kandidat dan lisensi

| Kandidat | Peran | Lisensi repo yang diperiksa | Kewajiban distribusi utama | Risiko maintenance | Kecocokan MatePad/HarmonyOS |
|---|---|---|---|---|---|
| [Box64](https://github.com/ptitSeb/box64) | Translasi x86_64 Linux userspace ke ARM64 | MIT (LICENSE) | Pertahankan notice dan teks lisensi | ABI host, dynarec, libc, upstream berubah | ARM64 cocok secara arsitektur; build Snapdragon 888 belum membuktikan Android/Bionic atau HarmonyOS |
| [Wine](https://github.com/wine-mirror/wine) | Windows API compatibility | LGPL-2.1-or-later (LICENSE; komponen tertentu bisa berbeda) | Sertakan LGPL, notice, source code yang diwajibkan untuk versi/modifikasi; audit per-file | Wine version, prefix, DLL, driver saling terkait | Wine di Linux bukan bukti bisa berjalan di HarmonyOS app sandbox |
| [DXVK](https://github.com/doitsujin/dxvk) | D3D8/9/10/11 ke Vulkan | zlib/libpng (LICENSE) | Pertahankan notice; tandai modifikasi; jangan salah klaim asal | Bergantung Vulkan loader/driver, Wine dan ekstensi Vulkan | Adreno 660 punya kapabilitas Vulkan di sejumlah perangkat, tetapi jalur Vulkan aplikasi ini belum diuji |
| [Winlator app](https://github.com/brunodev85/winlator-app) | Orkestrasi Android + Wine/Box64/container | LGPL-2.1 (LICENSE repo) | Sertakan LGPL dan penuhi source/relinking obligations untuk komponen tercakup; audit submodule/binary | Fork membawa coupling upstream, patch lokal, build pipeline dan versi runtime | Kandidat praktis untuk feasibility di Android; bukan bukti HarmonyOS 4.2/MatePad |
| Stack Ghost Town sendiri | Launcher, installer, manifest, telemetry, profiles | Lisensi proyek ditetapkan terpisah; dependency tetap tunduk lisensi masing-masing | SBOM, attribution, source offer/relocation bila LGPL/GPL berlaku; jangan bundle game | Beban integrasi, debugging ABI, rendering dan runtime tertinggi | Belum terbukti; Wineboot CI saat ini gagal EGL/SIGSEGV |

Lisensi repo bukan jaminan seluruh binary/submodule release memakai lisensi sama. Sebelum redistribusi, pin commit/tag, audit submodule, LICENSE/NOTICE, build flags, firmware/driver blobs dan dependency transitif. Wine LGPL dapat memiliki komponen/pengecualian berbeda; audit file-per-file.

## Perbandingan pendekatan

1. **Bangun seluruh stack sendiri.** Kontrol UX, manifest, permission dan logging paling besar, tetapi mengulang pekerjaan container manager, Wine packaging, controller mapping dan GPU driver. Risiko teknis sangat tinggi.
2. **Fork proyek sejenis (Winlator sebagai kandidat uji).** Memperoleh baseline Android/Wine/Box64 dan konfigurasi per game lebih cepat. Kekurangan: keputusan upstream mungkin tidak cocok, fork tidak menjamin HarmonyOS, dan kewajiban LGPL/dependency audit harus dipenuhi. Risiko sedang-tinggi.
3. **Hybrid dengan komponen pinned.** Pertahankan launcher/Play gate/manifest Ghost Town; gunakan komponen berlisensi jelas dengan versi, URL, SHA-256, provenance dan license bundle. Prefix dibangun per kombinasi Wine/runtime. Risiko sedang-tinggi; integrasi Android graphics/lifecycle tetap perlu dibuktikan.

## Rekomendasi sementara

Jangan memilih fork atau rewrite sekarang. Jalankan feasibility spike Winlator di perangkat target terlebih dahulu, lalu pilih hybrid atau fork. Ini bukan klaim Winlator telah diuji pada MatePad.

- Jika Winlator stabil dan Overcooked! 2 berjalan di MatePad, jadikan hybrid baseline: pertahankan UX/Play gate Ghost Town dan pin komponen yang benar-benar terbukti. Fork penuh hanya bila kebutuhan arsitektur tidak dapat diadaptasi.
- Jika Winlator gagal karena batas OS/GPU yang sama, fork tidak menyelesaikan masalah; isolasi satu variabel runtime/driver atau evaluasi jalur Vulkan Android.
- Jika Winlator berhasil tetapi komponen tidak dapat dipisah secara legal/teknis, evaluasi fork setelah audit lisensi penuh.

| Pilihan | Risiko estimasi | Alasan |
|---|---|---|
| Stack sendiri | Sangat tinggi | Semua lapisan dan kompatibilitas perangkat harus dibuktikan |
| Fork Winlator | Sedang-tinggi | Baseline matang, tetapi coupling upstream dan audit LGPL |
| Hybrid pinned | Sedang-tinggi | Batas komponen terukur; Android graphics tetap belum terbukti |

## Gerbang keputusan

1. Uji Winlator dan Overcooked! 2 di MatePad sesuai feasibility-spike.md.
2. Catat versi Wine, Box64, DXVK, driver, log, FPS, suhu, crash dan sesi 15 menit.
3. Pisahkan kegagalan app sandbox, wineserver/prefix, Vulkan, DXVK dan game.
4. Pilih arsitektur setelah bukti perangkat dan audit lisensi versi distribusi tersedia.
5. Sampai disetujui: jangan ubah runtime aktif, jangan aktifkan manifest, dan jangan hapus display gate.

## Referensi primer

- Box64/LICENSE: https://github.com/ptitSeb/box64
- Wine/LICENSE: https://github.com/wine-mirror/wine/blob/master/LICENSE
- DXVK/LICENSE: https://github.com/doitsujin/dxvk
- Winlator app/LICENSE: https://github.com/brunodev85/winlator-app
- Android Vulkan: https://developer.android.com/games/develop/vulkan
- Android graphics architecture: https://source.android.com/docs/core/graphics/architecture
- Huawei developer documentation: https://developer.huawei.com/consumer/en/doc/

## Klaim belum terverifikasi

- Winlator dapat diinstal/dijalankan pada HarmonyOS 4.2 di MatePad Air 2023.
- Turnip/Adreno driver bekerja pada GPU Adreno 660 melalui app sandbox perangkat ini.
- Vulkan loader, ekstensi Vulkan, DXVK dan Wine memiliki jalur kompatibel di perangkat ini.
- Overcooked! 2 build tertentu berjalan stabil dan dapat dimainkan.
- Moving Out 2 berjalan; ini target tambahan, bukan kriteria awal.
- Semua dependency/submodule/binary release Winlator mengikuti lisensi repo utamanya.
