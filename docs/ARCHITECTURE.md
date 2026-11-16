# Arsitektur Teknis `overcooked-recomp`

## 1. Perbandingan dengan `pes2017-recomp`
`pes2017-recomp` menerjemahkan instruksi mesin PowerPC Xbox 360 dan command buffer D3D9/Xenos ke C++ ARM64 dan Vulkan. Sebaliknya, **Overcooked** adalah game **Unity Engine** yang logika gameplay-nya berada di bytecode C# (.NET CIL di `Assembly-CSharp.dll` pada build Mono, atau metadata + native code pada build IL2CPP).

Oleh karena itu, `overcooked-recomp` mengadopsi **arsitektur hibrida**:
1. **Execution Runtime**:
   - **Jalur B (MVP — Mono ARM64 VM)**: Memuat `Overcooked_Data/Managed/Assembly-CSharp.dll` secara langsung ke domain Mono ARM64 di Android NDK dan menjembatani *internal calls* (`UnityEngine.*`) ke C++ NDK.
   - **Jalur A (Target Jangka Panjang — IL2CPP Metadata Reinterpretation)**: Mem-parsing `global-metadata.dat` (`0xFAB11BAF`), merekonstruksi logika metode dari build PC via Cpp2IL, dan mengompilasinya ulang secara AOT ke `arm64-v8a`.
2. **Vulkan TBDR Backend & Adreno Mitigations**:
   - Mengadopsi mitigasi Adreno 735/750 dari `pes2017-recomp`: pembatasan *storage buffer range* maksimal `128 MB` dan *triple-buffered swapchain* untuk mencegah *black frames*.
   - Mengoptimalkan *Render Pass* untuk arsitektur *Tile-Based Deferred Rendering* (TBDR) Adreno dengan `VK_ATTACHMENT_LOAD_OP_CLEAR` dan `VK_ATTACHMENT_STORE_OP_DONT_CARE` pada *depth attachment*.

## 2. Aliran Data Eksekusi
1. `GameStorageVerifier` memvalidasi integritas folder `Overcooked_Data/` milik pengguna (SHA-256) dan mendeteksi varian Mono atau IL2CPP.
2. `SerializedFileParser` dan `TextureTranscoder` mengekstrak mesh level serta mengonversi tekstur PC (DXT1/DXT5/BC7) menjadi **ASTC 6x6** atau **ETC2**.
3. `ShaderBridge` memetakan shader Unity ke bytecode **SPIR-V** yang dikompilasi untuk Vulkan mobile.
4. `NativeEngine` menjalankan loop frame utama di C++20, mengoordinasikan `UnityRuntimeHost`, `AAudioMixer`, `InputMapper`, dan `ThermalGovernor`.
