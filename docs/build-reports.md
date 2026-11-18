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
