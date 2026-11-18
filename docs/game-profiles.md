# Profil game — rancangan dan format

Profil adalah konfigurasi runtime per game, bukan file game. Nilai contoh Overcooked! 2 dan Moving Out 2 berstatus untested; bukan rekomendasi performa.

Schema: profiles/game-profile.schema.json. Validator: tools/validate_game_profile.py. Jalankan:
python3 tools/validate_game_profile.py profiles/overcooked-2.json
python3 tools/validate_game_profile.py profiles/moving-out-2.json
python3 -m unittest discover -s tools/tests -p 'test_game_profiles.py' -v

Deteksi: tools/detect-unity-build.py <folder-game> --output detection.json. Script hanya memeriksa nama/path dan baris log API grafis, tidak menyalin asset atau executable.
- *_Data/Managed/Assembly-CSharp.dll atau MonoBleedingEdge: indikasi Mono.
- GameAssembly.dll: indikasi IL2CPP.
- Keduanya: ambiguous, perlu inspeksi manual.
- Player.log: cari Direct3D 11/12, Vulkan, OpenGLCore. Tanpa indikator: unknown.
Hasil masuk ke profile.detection. Jangan otomatis memilih DXVK/driver sebelum API dan dukungan GPU diverifikasi.
