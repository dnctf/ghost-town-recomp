# Laporan build otomatis

Workflow build memanggil reusable workflow laporan pada job terakhir dengan kondisi always(). Job laporan mengunduh artifact yang tersedia, membaca metadata run/log parsial, mencetak quote-box ke Step Summary, dan mengunggah artifact build-report-<workflow>-<run_number> dengan retention 30 hari. Laporan per run tidak di-commit ke main sehingga tidak memicu CI loop.

## Cara membaca laporan
1. Hasil keseluruhan merangkum hasil job yang dikirim caller. TIDAK TERSEDIA berarti data tidak ditemukan, bukan nol atau sukses.
2. Ringkasan tahap memakai needs.*.result. Job yang tidak mulai karena dependency gagal dapat berstatus skipped.
3. Smoke ladder hanya diisi dari log rung yang ditemukan. Tidak ada log bukan PASS.
4. Kegagalan menunjukkan petunjuk log yang tersedia. Pisahkan bukti dari hipotesis; akar masalah yang belum dibuktikan harus disebut belum diketahui.
5. Versi, ukuran, hash, cache dan performa hanya ditulis bila ditemukan. Data hilang ditulis TIDAK TERSEDIA.
6. R/P/D/G membedakan bukti CI, emulator dan MatePad. APK sukses tidak membuktikan display/game berjalan.
7. Periksa redaksi sebelum membagikan laporan. Script tidak melakukan dump environment umum dan tidak boleh mengumpulkan token, serial, path pribadi atau isi game.

Setiap baris dibungkus quote Markdown agar mudah disalin. GHOST_TOWN_DISPLAY_READY=1 tetap wajib; report tidak pernah membuat marker.

## Mengambil laporan
Buka Actions > run > Artifacts dan unduh build-report-<workflow-slug>-<run_number>. Ringkasan yang sama ada di GitHub Actions Step Summary. Artifact build asli tetap terpisah.

## Batasan
GitHub dapat menghentikan runner secara paksa saat run dibatalkan atau platform tidak tersedia. always() berusaha menghasilkan laporan pada failure/timeout/cancelled, tetapi tidak menjamin upload bila report job ikut dihentikan paksa. Ketiadaan laporan harus dicatat sebagai tidak tersedia, bukan sukses.


## Sumber waktu, status rung, dan ukuran

- Job laporan mengambil metadata melalui GitHub CLI yang memanggil GitHub Actions REST API: `GET /repos/{owner}/{repo}/actions/runs/{run_id}`, `.../jobs?per_page=100`, dan `.../artifacts?per_page=100`. Waktu mulai/selesai run dan timestamp per job berasal dari API. Durasi dihitung dari selisih timestamp API; durasi report-job yang masih berjalan saat laporan diambil diberi label `IN PROGRESS` dan elapsed saat capture.
- Jika pemanggilan API gagal, metadata yang memerlukan API ditulis `TIDAK TERSEDIA`; data log lokal tetap diproses. Nilai hilang bukan nol dan bukan PASS.
- Smoke ladder ditulis `N/A (tidak berlaku)` untuk workflow selain runtime/Wine. Pada workflow runtime, rung yang tidak memiliki log ditulis `NOT RUN`; rung yang mempunyai log mengambil exit code/durasi dari log tersebut.
- Versi komponen dibaca dari `VERSION` bila ada, `android/app/build.gradle`, `runtime/manifest.json`, serta log/artifact build. Status runtime pack diambil dari `runtime_pack.status`, bukan dari status deskriptif tingkat atas manifest.
- Ukuran APK dibaca dari `archive_bytes` dalam `apk-size-audit`; SHA-256 dicatat dari langkah `sha256sum`. Untuk workflow APK, report mencari run sukses sebelumnya dari workflow yang sama, mengambil artifact `apk-size-audit`, dan menghitung delta release APK. Nomor run serta ukuran saat ini/sebelumnya dicantumkan sebagai sumber. Bila baseline/artifact tidak ada, delta ditulis `TIDAK TERSEDIA`.
- Daftar artifact dan byte berasal dari API artifacts. File log yang ditampilkan dipotong menjadi 50 baris terakhir dan disanitasi dari token serta path runner yang umum. Jangan pernah menaruh isi game, credential, atau path pribadi di log CI.
- `tools/tests/test_build_report.py` menguji workflow APK (smoke ladder N/A), runtime gagal di tengah (rung selanjutnya NOT RUN), dan API gagal (timestamp TIDAK TERSEDIA), selain kasus sukses dan timing API.
- `tools/tests/test_feasibility_ingest.py` menguji parser dan matriks keputusan untuk hasil pengguna. Ingest tidak mengubah profil atau memilih arsitektur otomatis.
