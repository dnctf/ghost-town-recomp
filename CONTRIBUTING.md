# Contributing

Thanks for helping with Ghost Town Recomp.

## Scope and safety
- Do not commit proprietary game executables, Unity assets, user saves, credentials, or runtime binaries without verified redistribution rights.
- Keep optional Windows compatibility components isolated from the Android-native Unity inspection code.
- Never report a data scan, CI build, or process launch as successful gameplay.
- Include source provenance, exact upstream revision, license, notices, and reproducible build steps for every third-party runtime component.

## Before submitting
1. Run `python3 -m unittest discover -s tools/tests -v`.
2. Run the Android workflow or local Gradle build.
3. Attach a probe JSON for any game-installation report.
4. For runtime claims, include target device, game build, backend, runner/runtime versions, logs, first rendered frame, input/audio results, and stability metrics.

## Tester reports
Open a GitHub issue and include:
- APK version and commit SHA
- Device / OS / SoC / GPU driver
- Game title, source build and Mono/IL2CPP classification
- Probe JSON (no proprietary binaries)
- Logcat and reproducible steps
- Separate pass/fail for process startup, rendering, gameplay, controls, audio and stability
