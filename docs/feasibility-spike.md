# Feasibility spike perangkat — MatePad Air 2023

Tujuan: baseline proyek Android/Wine matang sebelum mengubah runtime Ghost Town. Ini prosedur; belum ada hasil pengujian perangkat.

## Persiapan
- Konfirmasi model, HarmonyOS/build number, patch keamanan, RAM dan ruang kosong melalui Settings > About tablet.
- Unduh Winlator hanya dari https://github.com/brunodev85/winlator/releases. Catat versi, nama APK, URL dan SHA-256.
- Cadangkan save game. Gunakan salinan Overcooked! 2 yang sah milik pengguna. Jangan unggah executable, asset atau save ke issue/artifact.
- Uji Overcooked! 2 dahulu. Moving Out 2 adalah sesi terpisah setelahnya.

## Langkah uji Winlator
1. Catat model, HarmonyOS build, patch, RAM dan storage kosong.
2. Unduh APK release resmi Winlator; catat versi dan SHA-256 di komputer dengan sha256sum.
3. Instal APK. Jika sistem menolak, catat pesan persis; jangan menonaktifkan proteksi yang tidak diperlukan.
4. Buka Winlator dan tunggu first-run selesai. Catat Wine, Box64, DXVK/VKD3D, Turnip/Mesa, Vulkan driver dan renderer yang benar-benar ditampilkan.
5. Buat satu container x86_64 dengan default rekomendasi aplikasi. Catat resolusi, driver grafis, DX wrapper, Box64 preset dan semua environment variable.
6. Salin/instal Overcooked! 2 ke storage yang dapat diakses container. Jangan masukkan file game ke laporan.
7. Buat shortcut game dan mulai dari preset default. Catat API grafis dari log, apakah frame game tampil, input bekerja dan audio terdengar.
8. Mainkan tutorial/level lokal 15 menit. Catat FPS tiap menit, suhu, RAM, throttling, stutter, crash dan kehilangan input. Hentikan bila suhu/peringatan sistem tidak wajar.
9. Tutup normal, buka kembali, lalu uji pause/resume dan foreground/background jika aman.
10. Ekspor log aplikasi bila tersedia. Redaksi username, serial, path pribadi, token dan isi file game.

## Data wajib
| Kelompok | Data |
|---|---|
| Perangkat | Model, SoC, GPU, RAM, HarmonyOS build, patch |
| Aplikasi | Winlator version, release URL, APK SHA-256 |
| Runtime | Wine version/build, Box64 version/commit/preset, distro/container, prefix architecture |
| Grafik | Turnip/Mesa, Vulkan device/driver, DXVK, VKD3D, renderer, API game |
| Environment | BOX64_*, WINE*, DXVK_*, MESA_*, VK_* yang digunakan; jangan sertakan secret |
| Game | Judul/version/build, Mono/IL2CPP bila terdeteksi, API grafis; tanpa file game |
| Performa | FPS min/median, resolusi, scale, suhu awal/puncak/akhir, RAM awal/puncak/akhir |
| Stabilitas | Durasi, crash/hang, stutter, audio, input, suspend/resume |
| Bukti | Screenshot, log Wine/Box64/DXVK/Vulkan yang disanitasi |

## Template hasil
| Waktu UTC | Sesi | Resolusi/scale | Wine | Box64 | DXVK | Driver GPU | FPS min/median | Suhu awal/puncak | RAM awal/puncak | Crash/gejala | Log/bukti |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Belum diuji | Overcooked! 2 | — | — | — | — | — | — | — | — | — | — |
| Belum diuji | Moving Out 2 (opsional) | — | — | — | — | — | — | — | — | — | — |

Checklist: instalasi/first-run, container start/stop, frame game terlihat, audio, touch/keyboard/gamepad, FPS/suhu tercatat, 15 menit tanpa crash fatal, save tetap ada setelah restart, log disanitasi.

## Kriteria keputusan
- PASS baseline: game mencapai menu dan gameplay, audio/input bekerja, sesi 15 menit tanpa crash fatal, versi dan log tercatat.
- PARTIAL: container berjalan tetapi game blank/crash atau input/audio gagal.
- FAIL: aplikasi/container tidak dapat start atau OS menolak komponen.
- Jangan menyimpulkan kompatibilitas dari FPS menu saja. Ulangi perubahan satu variabel setiap percobaan.
Jangan kirim APK proprietary, game executable, asset, save data, serial, akun, token atau path pribadi.
