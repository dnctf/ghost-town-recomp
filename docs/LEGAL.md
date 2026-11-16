# Legal Notice, Copyright Policy & Compliance (`LEGAL.md`)

## 1. Penafian Afiliasi (Non-Affiliation Disclaimer)

**`overcooked-recomp`** adalah proyek penelitian perangkat lunak sumber terbuka (*open-source*) yang dikembangkan secara independen. Proyek ini **tidak berafiliasi, tidak disponsori, tidak didukung, dan tidak memiliki hubungan resmi** dengan:

* **Ghost Town Games Ltd.** (pengembang asli *Overcooked*)
* **Team17 Digital Ltd.** (penerbit *Overcooked*)
* **Unity Technologies** (pemilik hak cipta Unity Engine dan format aset terkait)
* **Qualcomm Technologies, Inc.** atau pemegang merek dagang lainnya yang disebutkan dalam dokumentasi teknis.

Semua nama produk, logo, merek dagang, dan merek dagang terdaftar yang disebutkan dalam repositori ini merupakan hak milik dari masing-masing pemegangnya dan digunakan semata-mata untuk tujuan identifikasi teknis serta kompatibilitas perangkat lunak.

---

## 2. Kebijakan *Bring Your Own Game* (BYOG)

Proyek `overcooked-recomp` berfungsi secara eksklusif sebagai lapisan *runtime* native Android, jembatan API grafis/audio (Vulkan & AAudio), serta alat konversi aset (*pipeline*) yang beroperasi di perangkat pengguna.

1. **Kepemilikan Sah Wajib**: Untuk menggunakan perangkat lunak yang dibangun dari repositori ini, pengguna **wajib memiliki salinan resmi dan sah** dari permainan *Overcooked* versi PC (yang diperoleh melalui saluran distribusi resmi seperti Steam, GOG, atau Epic Games Store).
2. **Pemrosesan Lokal**: Seluruh proses ekstraksi aset (`SerializedFile`), rekompilasi *shader* ke SPIR-V, serta kompresi tekstur ke ASTC/ETC2 dilakukan secara lokal pada perangkat pengguna menggunakan berkas instalasi milik pengguna sendiri.
3. **Tidak Ada Bypass DRM**: Proyek ini **tidak** menyertakan, mendukung, atau memfasilitasi mekanisme apa pun untuk melewati (*bypass*) perlindungan hak cipta atau *Digital Rights Management* (DRM) seperti Steamworks API atau perlindungan serupa.

---

## 3. Larangan Distribusi Konten Berhak Cipta

Repositori ini **tidak mengandung** dan **dilarang keras mendistribusikan** aset, biner, atau kode sumber milik pemegang hak cipta asli dalam bentuk apa pun, termasuk namun tidak terbatas pada:

* Direktori `Overcooked_Data/` atau arsip distribusi permainan asli.
* Berkas kode terkompilasi maupun metadata permainan asli (`Assembly-CSharp.dll`, `Assembly-CSharp-firstpass.dll`, `GameAssembly.dll`, `global-metadata.dat`).
* Aset visual, model 3D (`Mesh`), tekstur (`Texture2D`), animasi, tata letak level (`level0`–`levelN`), maupun berkas suara dan musik (`AudioClip` / FMOD banks).
* Paket instalasi Android (`.apk` atau `.aab`) yang telah dibundel bersama aset atau pustaka biner permainan asli.

---

## 4. Rekayasa Balik *Clean-Room* & Interoperabilitas

Implementasi *shim* C++ NDK pada `runtime/interop/`, `assets-pipeline/`, dan `shader-bridge/` dikembangkan menggunakan metode **clean-room implementation** untuk tujuan interoperabilitas perangkat lunak:

* **Spesifikasi Antarmuka**: Lapisan *interop* mengimplementasikan ulang perilaku fungsi standar engine (seperti transformasi ruang 3D, *render pass* Vulkan, dan simulasi fisika melalui Jolt Physics) berdasarkan perilaku publik dan dokumentasi antarmuka pemrograman aplikasi (API) yang terdokumentasi secara terbuka.
* **Tidak Ada Penyalinan Kode Sumber**: Tidak ada satu pun bagian dari kode sumber dekompilasi permainan asli yang disalin ke dalam basis kode repositori ini.

---

## 5. Verifikasi Checksum Berkas Pengguna

Untuk memastikan stabilitas *runtime* dan mencegah kesalahan *parsing* akibat versi permainan yang tidak didukung, aplikasi ini dapat menyertakan daftar nilai hash kriptografis (seperti SHA-256 atau CRC32) untuk memvalidasi integritas berkas yang disediakan pengguna.

* Nilai hash tersebut hanyalah **sidik jari matematis satu arah** (*one-way cryptographic digest*) yang digunakan untuk memvalidasi kompatibilitas struktur berkas lokal.
* Nilai hash tersebut tidak mengandung data berhak cipta dan tidak dapat digunakan untuk merekonstruksi aset maupun biner permainan asli.

---

## 6. Aturan Kontribusi (Pull Requests & Issues)

Setiap kontributor yang mengajukan *Pull Request* (PR), membuka *Issue*, atau berpartisipasi dalam diskusi repositori wajib mematuhi ketentuan berikut:

1. **Dilarang Mengunggah Hasil Dekompilasi**: Jangan menempelkan (*paste*) cuplikan kode C# hasil dekompilasi (misalnya dari ILSpy, dnSpy, Il2CppDumper, atau Cpp2IL) yang berasal dari `Assembly-CSharp.dll` atau `GameAssembly.dll` di dalam kode sumber, komentar, maupun deskripsi *Issue*. Gunakan referensi abstrak atau deskripsi perilaku fungsional.
2. **Dilarang Membagikan Tautan Pembajakan**: Segala bentuk tautan menuju situs unduhan ilegal, *repack*, atau distribusi aset permainan berhak cipta akan segera dihapus dan akun pelanggar akan diblokir dari repositori.
3. **Lisensi Kontribusi**: Dengan mengirimkan kontribusi kode ke repositori ini, kontributor menyatakan bahwa kode tersebut adalah karya orisinal mereka sendiri dan setuju untuk melisensikannya di bawah lisensi sumber terbuka proyek ini.

---

## 7. Lisensi Komponen Pihak Ketiga

Proyek `overcooked-recomp` memanfaatkan pustaka sumber terbuka pihak ketiga untuk mendukung eksekusi native di Android. Masing-masing komponen tetap tunduk pada lisensi aslinya:

* **Mono Runtime** — Dilisensikan di bawah *MIT License* / *LGPLv2*.
* **Jolt Physics** — Dilisensikan di bawah *MIT License*.
* **Vulkan Memory Allocator (VMA)** — Dilisensikan di bawah *MIT License*.
* **ARM ASTC Encoder (`astcenc`)** — Dilisensikan di bawah *Apache License 2.0*.
* **Khronos `glslang` & `SPIRV-Cross`** — Dilisensikan di bawah *Apache License 2.0* / *BSD-3-Clause*.

---

## 8. Pemberitahuan Hak Cipta & Prosedur Penghapusan (DMCA)

Pengelola proyek menghormati hak kekayaan intelektual seluruh pihak. Apabila Anda adalah pemegang hak cipta atau perwakilan sah dan meyakini bahwa terdapat materi di dalam repositori ini yang melanggar hak cipta Anda, silakan ajukan pemberitahuan resmi melalui tab **Security / Issues** pada repositori GitHub ini dengan menyertakan:

1. Identifikasi karya berhak cipta yang diklaim telah dilanggar.
2. Lokasi spesifik (URL atau *path* berkas) dari materi yang dimaksud di dalam repositori.
3. Informasi kontak resmi serta pernyataan itikad baik bahwa penggunaan materi tersebut tidak diizinkan oleh pemilik hak cipta.

Materi yang terbukti melanggar akan segera ditinjau dan dihapus dari riwayat repositori.
