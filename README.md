# Ghost Town Recomp (Android)

> **STATUS: Alpha — belum dapat memainkan game.**

Ghost Town Recomp adalah proyek launcher Android dan riset runtime untuk menjalankan game PC Windows secara eksperimental pada perangkat ARM64. Sasaran pengembangan saat ini adalah Overcooked! 2 pada Huawei MatePad Air 2023. Status di bawah hanya mencerminkan hasil CI yang benar-benar tercatat; belum ada klaim bahwa game dapat diluncurkan atau dimainkan.

## Status gerbang

| Gerbang | Status | Bukti CI | Emulator | MatePad Air 2023 |
|---|---|---|---|---|
| **R — Runtime CI** | **Gagal / belum hijau** | Run [#44243158612](https://github.com/dnctf/ghost-town-recomp/actions/runs/44243158612): a–c PASS; d `wineboot` timeout 180s dengan SIGSEGV/EGL; e NOT RUN. Harness diagnostik diperkuat; menunggu dispatch 600s | Belum diuji | Belum diuji |
| **P — Runtime pack & installer** | **Belum diuji; manifest pending** | Belum ada archive runtime terverifikasi atau asset release | Belum diuji terhadap archive nyata | Belum diuji |
| **D — Display X11/VirGL** | **Belum diuji** | Belum ada self-test window/GLX/frame yang lolos | Belum diuji | Belum diuji |
| **G — Game** | **Belum diuji** | Belum ada D3D11/game/audio/input/stability evidence | Belum diuji | Belum diuji |

Run [UI Preview #44238289772](https://github.com/dnctf/ghost-town-recomp/actions/runs/44238289772) berhasil membangun APK preview, menjalankan lint dan empat unit test Play policy. Keberhasilan itu bukan bukti runtime atau game dapat berjalan.

## Yang sudah berfungsi / terbukti

- **UI Preview** berbasis Jetpack Compose dapat dibangun di CI sebagai APK debug ARM64. APK berukuran 27.157.234 byte (25,90 MiB), package ID memakai suffix `.uipreview`, dan audit APK melaporkan `EMBEDDED_RUNTIME_PARTS=0`.
- Dashboard demo, halaman Runtime, Library (pemilih folder SAF), dan Settings telah dikompilasi serta melewati lint di CI. Empat unit test Play launch policy lulus.
- **Play gate nyata** membaca status instalasi dari `RuntimeBundleInstaller.isInstalled()` dan marker display `filesDir/runtime/display/GHOST_TOWN_DISPLAY_READY` dengan isi persis `GHOST_TOWN_DISPLAY_READY=1`. Preview tidak membuat marker; Play tetap diblokir sampai self-test display sungguhan menulis marker tersebut.
- Pipeline runtime CI telah berhasil membangun Box64 ARM64 di container Debian Bookworm. Pemeriksaan simbol mencatat kebutuhan maksimum Box64 `GLIBC_2.35`, sementara rootfs Bookworm menggunakan glibc `2.36`; pemeriksaan kompatibilitas ini lulus pada run #46.
- Bootstrap rootfs, download/extract paket amd64, validasi Wine ELF/dependency, dan verifikasi glibc lulus pada run #48. Run #48 membuktikan Box64 `v0.4.5`, Wine `11.19`, dan rung a–c PASS pada rootfs untrimmed. Rung d `wineboot` timeout setelah 180 detik; log memuat kegagalan EGL dan SIGSEGV di `win32u.so/parse_extensions`. Rung e belum dijalankan.

## Yang belum berfungsi / belum terbukti

- Runtime pack belum dibangun, belum memiliki URL/ukuran/SHA-256 final, dan belum dipublikasikan. `runtime/manifest.json` tetap `runtime_pack.status = pending`.
- Pengunduh/installer belum diuji terhadap archive runtime nyata end-to-end (resume, retry, checksum, ekstraksi atomik, pemulihan instalasi rusak, dan low-storage).
- Wine belum lulus smoke ladder a-e: a–c PASS di CI, d timeout dengan SIGSEGV/EGL, e NOT RUN. Belum ada X11 window, GLX renderer, VirGL visible frame, D3D11 frame, atau marker display-ready.
- Peluncuran Overcooked! 2, input sentuh/keyboard/gamepad ke Wine, audio game, pause/resume, crash recovery, FPS, RAM, suhu, dan sesi stabil 15 menit belum diuji.
- Fitur **Ekspor diagnostik ZIP** belum tersedia.
- Tidak ada pengujian emulator atau pengujian fisik Huawei MatePad Air 2023 yang diklaim.

## Arsitektur target

Runtime dipisah dari APK dan diunduh setelah runtime pack tersedia:

```text
Android App
   |
   +-- UI / Library (SAF) / Runtime manager
   |
   +-- download + verify runtime pack (HTTPS + SHA-256)
          |
          v
       App-private rootfs
          |
        PRoot
          |
        Box64 (ARM64 host -> x86_64 guest)
          |
        Wine (Windows x86_64)
          |
        X11 / guest Mesa / VirGL transport
          |
        Android display SurfaceView
```

APK hanya membawa komponen Android/native yang memang diperlukan (termasuk PRoot ARM64). **Rootfs, Box64 guest payload, Wine, dan file game tidak dibundel ke APK UI Preview.** Arsitektur diagram adalah target; jalur tampilan belum lolos Gerbang D.

## Cara mencoba build yang tersedia sekarang

1. Buka [UI Preview workflow](https://github.com/dnctf/ghost-town-recomp/actions/workflows/ui-preview.yml).
2. Pilih run sukses [#44238289772](https://github.com/dnctf/ghost-town-recomp/actions/runs/44238289772), lalu unduh artifact `ghost-town-ui-preview-0.5.0-alpha.2-60800bfda51bf8d858f36ddf602aa809d0ba6795`.
3. Ekstrak ZIP dan instal APK:
   ```bash
   adb install -r app-debug.apk
   adb shell monkey -p com.overcookedrecomp.app.uipreview 1
   ```
4. Yang dapat diuji sekarang: navigasi dashboard demo, detail kartu, tab Runtime/Library/Settings, pemilih folder SAF, dan perilaku gate Play yang tetap blocked. Kartu game masih data demo; ini bukan peluncur game.

Petunjuk rinci ada di [docs/ui-preview.md](docs/ui-preview.md). APK ini debug-signed dan bukan distribusi Play Store.

## Build dan alur CI

### Build Android lokal

Prasyarat: JDK 17, Android SDK/NDK yang sesuai konfigurasi Gradle, CMake 3.22.1, dan Gradle 8.2.2-compatible.

```bash
cd android
gradle assembleDebug
gradle lint
```

APK debug berada di `android/app/build/outputs/apk/debug/app-debug.apk`. Untuk workflow CI:

- [Android APK build & size audit](.github/workflows/build-android.yml)
- [UI Preview APK](.github/workflows/ui-preview.yml)
- [ARM64 Wine runtime bundle](.github/workflows/build-wine-runtime-bundle.yml)

Runtime workflow membangun Box64 pada Debian Bookworm ARM64, menyiapkan Debian Bookworm ARM64 + amd64 rootfs, memeriksa ELF/glibc, menjalankan smoke ladder sebelum trim, lalu mengemas archive jika semua gate berhasil. Run terbaru yang diperiksa tercatat di [runtime-debug](docs/runtime-debug.md).

### Versi dan manifest

Versi aplikasi/runtime saat ini masih `0.5.0-alpha.2` di konfigurasi Gradle dan manifest. **Sumber versi tunggal `VERSION` belum tersedia atau belum diintegrasikan**; penyatuan versi aplikasi, workflow, downloader, dan manifest adalah pekerjaan Gerbang P1. Jangan mengubah manifest menjadi aktif sebelum archive nyata selesai diverifikasi.

## Target perangkat dan risiko

Target pengujian: **Huawei MatePad Air 2023**, Snapdragon 888, Adreno 660, ARM64, HarmonyOS 4.2.

Risiko yang masih terbuka:

- Android/HarmonyOS W^X dan kebijakan eksekusi binary dari app sandbox.
- seccomp, PRoot/ptrace, akses `/proc`, `/dev/pts`, dan namespace/mount yang diizinkan.
- Tidak mengandalkan SysV shared memory; jalur grafis harus sesuai kemampuan host Android.
- Kecocokan Mesa `virtio_gpu_dri.so` dengan transport socket vtest/virpipe belum terbukti; rencana Vulkan/Turnip + DXVK masih ditunda.
- RAM, tekanan memori, panas, thermal throttling, dan konsumsi daya saat sesi panjang.

Dokumen teknis:
- [Runtime debug dan smoke ladder](docs/runtime-debug.md)
- [Risk register](docs/risk-register.md)
- [Graphics transport plan](docs/graphics-plan.md)
- [Device bring-up — draft, belum untuk dijalankan](docs/device-bringup.md)
- [UI Preview guide](docs/ui-preview.md)

## Roadmap berbasis gerbang

1. **R — Runtime CI:** buktikan rung a-e lengkap dengan log dan timeout; verifikasi usr-merge, loader, glibc, library path, dan mount chroot; baru trim, ukur, hash, dan publikasikan runtime archive.
2. **P — Runtime pack:** isi manifest dari archive terverifikasi, uji downloader/installer, tambahkan Ekspor diagnostik, bangun APK uji dan panduan device bring-up. Setelah P4, berhenti untuk pengujian MatePad oleh pengguna.
3. **D — Display:** setelah bukti perangkat Gerbang P diterima, buktikan xmessage, glxinfo -B, dan glxgears terlihat di SurfaceView. Marker hanya dibuat oleh self-test yang sukses. Setelah itu berhenti untuk uji display di MatePad.
4. **G — Game:** setelah display terbukti di perangkat, lanjut D3D11, input, audio, lifecycle, dan stabilitas game dengan jeda pengujian perangkat di setiap sub-gerbang.

## Cara melaporkan masalah

Fitur **Ekspor diagnostik ZIP direncanakan pada P3 tetapi belum tersedia**. Jangan mengirim file game atau data pribadi. Untuk kondisi build saat ini, sertakan:
- versi aplikasi dan model/versi OS perangkat;
- langkah reproduksi dan waktu kejadian;
- log CI yang relevan atau `adb logcat` yang sudah disaring dari informasi pribadi;
- screenshot UI bila masalahnya visual.

Pengguna menyediakan sendiri salinan Overcooked! 2 yang sah. Jangan mengunggah executable atau aset game ke issue/artifact proyek.

## Legal

Repositori ini **tidak menyertakan file Overcooked! 2**, executable game, maupun aset proprietary. Pengguna wajib menyediakan salinan game yang diperoleh secara sah. Proyek ini tidak berafiliasi, didukung, atau disponsori oleh **Team17, Ghost Town Games, Microsoft, atau Sony**. Aset UI Preview merupakan karya orisinal/prosedural untuk proyek ini; merek dan nama game tetap milik pemegang hak masing-masing.
