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


## Audit lisensi berbasis berkas LICENSE — diperiksa 2026-11-18

Kutipan di bawah disalin langsung dari berkas lisensi pada URL yang ditautkan. Tanggal adalah tanggal pemeriksaan repository/berkas pada sesi audit ini. Kutipan dibatasi sebagai cuplikan identifikasi; bukan pengganti teks lisensi lengkap. Audit ini bukan nasihat hukum.

### Aplikasi/orkestrator

**Winlator — `brunodev85/winlator`, branch `main`**

- Berkas: [LICENSE (raw)](https://raw.githubusercontent.com/brunodev85/winlator/main/LICENSE)
- Metadata repo: LGPL-2.1; berkas LICENSE memulai dengan:
  > GNU LESSER GENERAL PUBLIC LICENSE
  > Version 2.1, February 1999
- Cuplikan kewajiban dari berkas yang sama:
  > you must give the recipients all the rights that we gave you.
- Dampak distribusi: sertakan LGPL-2.1 lengkap dan copyright/notice; sediakan source code sesuai ketentuan LGPL untuk bagian yang didistribusikan/dimodifikasi; evaluasi kewajiban relinkability bila aplikasi menggabungkan library LGPL. Audit APK/release harus memeriksa dependency dan cara linking, bukan hanya root LICENSE.

**WinlatorXR — `WinlatorXR/WinlatorXR`, branch `cmod_bionic`**

- Berkas: [LICENSE (raw)](https://raw.githubusercontent.com/WinlatorXR/WinlatorXR/cmod_bionic/LICENSE)
- Cuplikan langsung:
  > The code added or modified in this fork is subject to the GPLv3 license.
  > upstream repository is licensed under the MIT license.
- Dampak distribusi: perubahan fork dinyatakan GPLv3, sedangkan kode upstream dinyatakan MIT oleh berkas fork. Jika mendistribusikan binary turunan, penuhi GPLv3 untuk bagian fork (termasuk source corresponding yang diwajibkan), pertahankan notice MIT untuk kode upstream, dan audit semua submodule. Metadata API repo memberi `NOASSERTION`; jangan menyederhanakan seluruh repo menjadi satu lisensi tanpa file-level review.

### Runtime / translasi

**Box64 — `ptitSeb/box64`, branch `main`**

- Berkas: [LICENSE (raw)](https://raw.githubusercontent.com/ptitSeb/box64/main/LICENSE)
- Cuplikan langsung:
  > The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
- Lisensi yang dinyatakan file: MIT. Distribusi perlu mempertahankan copyright dan permission notice. Periksa pula library/dependency yang ikut dikemas; lisensi Box64 tidak otomatis melisensikan semua runtime library.

**Wine — `wine-mirror/wine`, branch `master`**

- Berkas: [LICENSE (raw)](https://raw.githubusercontent.com/wine-mirror/wine/master/LICENSE); repo juga menyertakan [COPYING.LIB](https://github.com/wine-mirror/wine/blob/master/COPYING.LIB).
- Cuplikan langsung:
  > Wine is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License
- File LICENSE menyatakan LGPL versi 2.1 atau versi lebih baru dan menunjuk COPYING.LIB. Distribusi perlu menyertakan teks lisensi dan memenuhi kewajiban source/modifikasi LGPL yang relevan. Repository Wine juga memuat komponen pihak ketiga dengan berkas LICENSE/COPYING tersendiri; inventaris per komponen wajib sebelum membuat runtime pack.

**Wine-Builds — `Kron4ek/Wine-Builds`, branch `master` (kemasan binary yang dipakai manifest)**

- Berkas: [LICENSE (raw)](https://raw.githubusercontent.com/Kron4ek/Wine-Builds/master/LICENSE)
- Cuplikan langsung:
  > The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
- Metadata repository menyatakan MIT untuk repository build/packaging. Ini **tidak menggantikan** lisensi Wine upstream atau lisensi komponen yang terdapat dalam arsip Wine 11.19. Binary archive yang dipakai harus diperiksa notices dan provenance-nya secara terpisah.

**DXVK — `doitsujin/dxvk`, branch `master`**

- Berkas: [LICENSE (raw)](https://raw.githubusercontent.com/doitsujin/dxvk/master/LICENSE)
- Cuplikan langsung:
  > The origin of this software must not be misrepresented; you must not claim that you wrote the original software.
- Lisensi file: zlib/libpng. Pertahankan notice pada source distribution dan tandai perubahan source; bila binary didistribusikan, pertahankan attribution/notice sesuai lisensi dan dependency. DXVK belum dibundel dalam runtime saat ini.

### Driver GPU / loader

**Mesa / Turnip — `mesa/mesa`, branch `main`**

- Berkas repository: [LICENSE (raw)](https://raw.githubusercontent.com/mesa/mesa/main/LICENSE) dan [NOTICE (raw)](https://raw.githubusercontent.com/mesa/mesa/main/NOTICE).
- Cuplikan langsung dari [LICENSE](https://raw.githubusercontent.com/mesa/mesa/main/LICENSE):
  > Apache License Version 2.0, January 2004
- Cuplikan langsung dari [NOTICE](https://raw.githubusercontent.com/mesa/mesa/main/NOTICE):
  > Licensed under the Apache License, Version 2.0 (the "License");
- Metadata repository menunjukkan Apache-2.0 untuk berkas LICENSE tingkat repo. **Ini bukan bukti bahwa seluruh source Mesa/Turnip atau setiap binary driver hanya Apache-2.0**: Mesa memuat file/komponen dengan header lisensi berbeda. Sebelum bundling Turnip, audit source file yang masuk build, subproject, firmware, generated code, dan notices binary. Lisensi final driver build belum diverifikasi file-per-file.

**libadrenotools — `bylaws/libadrenotools`, branch `master`**

- Berkas: [LICENSE (raw)](https://raw.githubusercontent.com/bylaws/libadrenotools/master/LICENSE)
- Cuplikan langsung:
  > Redistributions in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
- Lisensi file: BSD 2-Clause. Distribusi source/binary harus mempertahankan copyright, conditions, dan disclaimer. Repo ini adalah loader/hook helper, bukan lisensi untuk firmware/driver Qualcomm yang dimuatnya.

**AdrenoToolsDrivers — `K11MCH1/AdrenoToolsDrivers`, branch `main`**

- Pemeriksaan tree repository pada 2026-11-18 tidak menemukan berkas bernama LICENSE/COPYING/NOTICE pada root atau tree yang diperiksa; metadata API memberi `NOASSERTION`.
- **Lisensi driver binary belum diverifikasi.** Jangan redistribusikan paket driver dari repo ini sampai lisensi/izin redistribusi tiap blob dan sumbernya dapat dibuktikan. Tidak ada kutipan lisensi yang dapat disalin karena berkas lisensi tidak ditemukan.

### Batas audit dan tindakan wajib sebelum distribusi

1. Kutipan di atas berasal dari berkas repository pada branch yang ditautkan, bukan pemeriksaan hukum atas semua binary release. Branch dapat berubah; pin commit dan simpan salinan LICENSE/NOTICE saat membangun.
2. Wine, Winlator, WinlatorXR, Mesa dan APK release membawa dependency/submodule/firmware yang lisensinya dapat berbeda. Buat SBOM per archive, audit semua submodule termasuk nested submodule, dan cocokkan setiap binary dengan source commit.
3. Simpan direktori `licenses/` di setiap komponen runtime, lengkap dengan source URL, commit, file LICENSE/COPYING/NOTICE, dan daftar perubahan. Untuk LGPL/GPL, siapkan source corresponding dan mekanisme pemenuhan kewajiban distribusi sebelum rilis publik.
4. Driver proprietary/firmware Qualcomm dan binary AdrenoToolsDrivers tidak boleh dianggap bebas redistribusi hanya karena helper/loader memiliki lisensi BSD.
5. Audit ini tidak mengubah keputusan arsitektur. Winlator/MatePad masih harus diuji oleh pengguna; status HarmonyOS 4.2 dan Adreno 660 tetap belum terverifikasi.
