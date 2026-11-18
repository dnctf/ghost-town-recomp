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


## Matriks keputusan pra-hasil (semua ambang adalah USULAN)

Matriks ini ditetapkan sebelum menerima hasil pengguna. Ini alat triase, bukan keputusan final arsitektur. Jangan memilih fork/rewrite sebelum tabel hasil dan log perangkat diterima.

| Hasil uji Winlator | Interpretasi awal (USULAN) | Tindak lanjut |
|---|---|---|
| Tidak terpasang atau aplikasi tidak dapat dijalankan pada HarmonyOS 4.2 | Blocker tingkat OS/sandbox sampai terbukti penyebab spesifik | Catat pesan error lengkap, HarmonyOS build, izin yang diminta, sumber APK dan SHA-256; cari penyebab sebelum memilih jalur. Jangan simpulkan semua Wine/Box64 mustahil dari satu error instalasi. |
| Terpasang, tetapi game tidak tampil atau crash saat start | Masalah jalur grafis/runtime/komponen (USULAN) | Simpan log dan identifikasi tahap terakhir; ulangi dengan satu perubahan per sesi: driver GPU atau DXVK, bukan keduanya sekaligus. |
| Menu tampil, FPS < 20 | Performa belum memadai; tuning (USULAN) | Uji skala resolusi dan preset performa satu per satu. Belum cukup untuk memutuskan arsitektur; bedakan FPS menu dari gameplay. |
| Gameplay >= 30 FPS stabil selama >= 15 menit tanpa crash fatal | Jalur hybrid pinned layak dievaluasi (USULAN) | Catat dan salin konfigurasi yang benar-benar terbukti ke catatan kandidat/profil Overcooked! 2. **Jangan ubah status profil Ghost Town dari `untested`** sampai stack Ghost Town sendiri membuktikan konfigurasi itu. |

### Format input untuk ingest

Gunakan tabel Markdown atau CSV dengan header berikut (alias umum juga diterima): `installed`, `game_visible`, `menu_visible`, `gameplay`, `fps_median`, `stable_minutes`, `crash`, `error`, `driver`, `dxvk`, `notes`.

Contoh CSV:

```csv
installed,game_visible,menu_visible,gameplay,fps_median,stable_minutes,crash,error,driver,dxvk,notes
yes,yes,yes,yes,32,15,no,,Turnip,off,contoh data saja
```

Jalankan:

```bash
python3 tools/ingest_feasibility.py hasil-matepad.csv
python3 tools/ingest_feasibility.py hasil-matepad.md --output ringkasan.md
python3 -m unittest tools.tests.test_feasibility_ingest -v
```

Tool hanya membaca tabel dan menghasilkan ringkasan/rekomendasi. Tool tidak mengedit profil game, manifest, runtime, atau memutuskan arsitektur otomatis. Semua angka ambang di atas adalah usulan dan harus dipertimbangkan bersama log/error serta pengulangan pengujian.
