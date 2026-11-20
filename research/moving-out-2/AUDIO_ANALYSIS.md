# Moving Out 2 — Audio Analysis

## Evidence

- **VERIFIED (baseline inventory):** `fmodstudio.dll` and `resonanceaudio.dll` are present.
- **INFERRED:** FMOD Studio integration and spatial-audio functionality are likely used by the Windows build.
- **NOT TESTED:** FMOD version, linked codec/plugins, audio device backend, sample rate, channel layout, DSP/plugin load, audio thread behavior, and whether Resonance Audio is required for startup.

## Ghost Town status

The runtime manifest marks audio/input as not implemented for the Wine process. The Android native project includes an AAudio-backed native source, but a native app audio engine is not automatically an audio bridge for a Wine guest process.

## Compatibility risks

Potential gaps include guest Windows audio APIs, Wine audio driver configuration, Android audio focus, device route changes, latency, sample-rate conversion, multichannel downmix, and process teardown. These are hypotheses until a real guest audio stream is captured.

## Validation

First verify Wine audio backend/device enumeration with a harmless guest audio test. Then test a short tone and capture underruns/latency. Only after that evaluate game audio, FMOD initialization, spatialization, focus loss, Bluetooth route changes, and cleanup. No proprietary audio assets are required.
