# Dev loop on-device (rencana)

Status: rancangan setelah keputusan arsitektur dan pemisahan komponen. Runtime belum diubah.

## Prinsip
- Host Linux/macOS/Windows dengan Android Platform Tools; tablet dengan ADB debugging yang diaktifkan pengguna.
- Uji komponen satu per satu; simpan versi dan SHA-256 sebelum/sesudah.
- Gunakan staging directory, checksum, atomic rename dan rollback.
- Jangan pernah menulis GHOST_TOWN_DISPLAY_READY=1 dari script. Hanya self-test display sukses boleh membuat marker.
- Jangan salin file game ke CI, artifact atau log.

## Alur
1. Jalankan adb devices dan pastikan device benar.
2. Instal debug APK dan ambil package/version.
3. Push archive komponen ke staging; aplikasi/dev helper memverifikasi SHA-256 dan metadata manifest.
4. Backup komponen aktif, ekstrak ke staging app-private, validasi ABI/dependency, lalu atomic switch.
5. Jalankan tes terpilih: Box64 version, Wine version, prefix sanity, display self-test. Wineboot tetap tes diagnosis terpisah, bukan syarat first-run jika prefix siap pakai tervalidasi digunakan.
6. Tarik logcat, diagnostic export, manifest komponen, hash, exit code dan durasi.
7. Jika gagal, restore pointer versi sebelumnya dan ulangi dengan satu variabel berbeda.

## Script yang direncanakan
tools/device-loop.sh (belum diimplementasikan) menyediakan list, push-component, activate, rollback, test, logs dan report. Push/activate harus memverifikasi nama komponen dari allowlist manifest dan SHA-256; jangan menerima path arbitrer.

ADB tidak otomatis memberi akses ke private app data. Jangan mengasumsikan run-as tersedia pada release build. Gunakan debug-only IPC terbatas atau diagnostic export aplikasi untuk operasi app-private. Jangan mengandalkan Termux membaca direktori private aplikasi lain.

Setiap percobaan menghasilkan device-test-<UTC>.json: model/OS, versi komponen, SHA-256, command, exit code, elapsed time, self-test, suhu/RAM bila tersedia, dan daftar log yang disanitasi.

## Rancangan lapisan runtime berversi (belum diterapkan)

Setelah arsitektur disetujui, manifest memuat komponen independen: base rootfs, Box64, Wine, driver GPU, DXVK (opsional), dan prefix Wine siap pakai. Setiap komponen wajib mempunyai id, version, target ABI/OS, URL HTTPS, SHA-256, byte size, license/provenance, serta compatibility constraints. Manifest tidak boleh aktif sebelum URL, ukuran dan hash berasal dari archive yang benar-benar dibangun dan diverifikasi.

APK/CI aplikasi hanya membangun launcher dan native Android app. Saat first-run, Runtime Manager mengunduh komponen pinned, memverifikasi SHA-256, mengekstrak ke staging, memvalidasi layout/dependency, lalu atomic activation. Versi prefix diikat ke Wine build, architecture dan runtime ABI; prefix archive dibangun sekali di CI atau dibuat pada perangkat, disimpan dengan hash dan diekstrak saat first-run. Prefix tidak boleh berisi data pengguna, credential, atau file game.

Regenerasi prefix: buat prefix baru pada runtime/Wine build yang persis dipin; jalankan wineboot dalam job eksperimen terpisah; pasang konfigurasi redistributable yang diperlukan; validasi registry/prefix layout dan startup minimal; arsipkan prefix tanpa game/user data; catat versi, ukuran, SHA-256 dan provenance. Jika Wine berubah, bangun prefix baru—jangan menganggap prefix lama kompatibel.

Wineboot tetap smoke test diagnostik terpisah, bukan syarat first-run/runtime bila prefix siap pakai telah lolos validasi. Rung wineboot tetap dicatat pada CI untuk diagnosis. Tidak ada perubahan ke gerbang display: hanya self-test display nyata yang boleh menghasilkan GHOST_TOWN_DISPLAY_READY=1.
