# Matriks Optimasi Per-Generasi Qualcomm Snapdragon

| Parameter & Metrik | Snapdragon 845 (Adreno 630) | Snapdragon 8 Gen 1/2 (Adreno 730/740) | Snapdragon 8 Gen 3+ (Adreno 750) |
|---|---|---|---|
| **Target Resolusi Internal** | 720p (Dinamis: 540p – 720p) | 900p – 1080p (Dinamis: 720p – 1080p) | 1080p – 1440p native |
| **Target FPS Minimum / Maksimum** | Min 30 FPS stabil (lock); 60 FPS di level ringan | Min 55 FPS / Target 60 FPS lock | Min 60 FPS / Target 120 FPS (layar LTPO) |
| **Batas Suhu Maksimum (Skin / SoC)** | <= 41 °C (Skin) / <= 75 °C (Junction) | <= 43 °C (Skin) / <= 82 °C (Junction) | <= 44 °C (Skin) / <= 85 °C (Junction) |
| **Konsumsi Daya Perkiraan** | 3.2 W – 4.2 W | 4.5 W – 5.8 W | 4.8 W – 6.5 W |
| **API Grafis** | Vulkan 1.1 (wajib); fallback GLES 3.2 | Vulkan 1.3 | Vulkan 1.3 + `VK_EXT_dynamic_rendering` |
| **Format Tekstur** | ASTC 6x6 (fallback ETC2) | ASTC 6x6 | ASTC 4x4 (UI/Hero) & ASTC 6x6 (Lingkungan) |
| **Anisotropic Filtering** | Maksimal 4x | 4x – 8x | 8x – 16x |
| **Strategi Termal** | `MODERATE` -> 612p + 30 FPS; `SEVERE` -> 540p + 30 FPS | `MODERATE` -> skala resolusi -15%; `SEVERE` -> 45/30 FPS | Manfaatkan headroom; frame gen hanya saat `NONE` |
| **Spin-Wait Frame Pacing** | Sleep hingga 100 µs saat FPS < 60 atau termal >= `MODERATE` | Sleep adaptif 50 µs | Minimal spin-wait; `sched_yield()` |
| **Alokasi Arena Memori** | RAM 4 GB -> arena 512 MB – 1 GB | RAM 8 GB -> arena 1.5 GB – 2 GB | RAM 12–16 GB -> arena 2 GB – 3 GB |
| **Perbaikan Spesifik Adreno** | `minUniformBufferOffsetAlignment`; pecah storage buffer > 128 MB | Batas storage buffer 128 MB & pipeline cache | Wajib guard storage buffer 128 MB (Adreno 735/750) + triple buffering |
