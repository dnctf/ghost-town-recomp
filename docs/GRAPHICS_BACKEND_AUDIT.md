# Windows game graphics backend — integration audit

## Why the current runtime cannot display a game yet

The current app has an Android `SurfaceView` for its native recompilation engine. That surface is not an X11 or Wayland display server. Wine applications expect a Windows display driver/windowing path, and DXVK expects a working Vulkan loader, ICD/driver, and presentation surface. Merely placing DXVK DLLs in the Wine rootfs does not provide those services.

The current runtime work covers:
- Android ARM64 PRoot process launcher
- ARM64 glibc rootfs
- ARM64 Box64
- x86_64 Wine and its amd64 shared libraries
- DXVK DLL payload
- app-private installation and a headless Wine version probe

It does **not** provide an X server, Wayland compositor, Wine display driver, Vulkan ICD bridge, audio server, or graphics presentation path.

## Reference implementation inspected

The WinlatorXR project (branch `cmod_bionic`) is a useful architecture reference, not a drop-in library:

- `app/src/main/java/com/winlator/cmod/XServerDisplayActivity.java` coordinates the container, X environment, graphics driver, audio, input, and display lifecycle.
- `app/src/main/java/com/winlator/cmod/widget/XServerView.java` hosts a `GLSurfaceView` and renderer for the X server display.
- `app/src/main/java/com/winlator/cmod/xenvironment/components/XServerComponent.java` runs the X11 socket server.
- `app/src/main/java/com/winlator/cmod/xenvironment/components/VirGLRendererComponent.java` connects the guest renderer to an EGL context shared with the Android renderer.
- `app/src/main/cpp/virglrenderer/` contains the VirGL renderer implementation and server.
- `app/src/main/cpp/winlator/vulkan.cpp` queries Android's system Vulkan loader; it is GPU information plumbing, not a complete glibc-to-Bionic Vulkan bridge.
- `app/src/main/assets/graphics_driver/` contains Turnip/adrenotools driver payloads and a `libvulkan_wrapper` payload.
- `app/src/main/java/com/winlator/cmod/xenvironment/components/PulseAudioComponent.java` and `app/src/main/java/com/winlator/cmod/audio/` show the audio-server/client architecture.

Reference repository: https://github.com/WinlatorXR/WinlatorXR/tree/cmod_bionic

## Licensing and reuse boundary

The reference repository describes its modified code as GPL-3.0 and upstream code as MIT. Individual driver archives and other binary payloads have their own provenance/licensing obligations. Do not copy its source files or binary driver archives into this repository until the exact file-level license, notices, and redistribution terms have been reviewed. The current plan is to use it as an architecture reference and independently build or explicitly attribute compatible components.

## Required implementation sequence

1. **Display contract:** define a Wine display backend interface that owns an Android surface, input dispatch, resize, pause/resume, and teardown.
2. **X11/windowing:** port or implement an X server and its socket protocol. A plain Android `SurfaceView` is not a substitute.
3. **Graphics transport:** choose either a proven VirGL/EGL path or a Vulkan path. Avoid mixing two unverified backends in the first milestone.
4. **Vulkan bridge:** provide a glibc-visible Vulkan loader/ICD route to the Adreno driver. A glibc process must not directly assume it can load Android's Bionic `/system/lib64/libvulkan.so`.
5. **Adreno 660 driver:** select a Turnip build only after checking its GPU generation, Vulkan extensions, Android API requirements, and license. Validate device creation and presentation on the MatePad Air.
6. **Wine integration:** configure Wine's display driver and DXVK DLL overrides per prefix; verify D3D11 device creation before testing a game.
7. **Audio/input:** connect Wine audio to an Android audio service and translate keyboard, mouse, touch, and gamepad events.
8. **Lifecycle:** manage wineserver, Box64, PRoot process groups, background/foreground transitions, force-stop, logs, and cleanup.
9. **Physical tests:** test a small Direct3D sample first, then one user-supplied 64-bit Unity game. Record rendering, input, audio, frame pacing, memory, thermals, and stability.

## Current status

No graphics backend is integrated. The launcher must continue to show runtime status/probe only and must not claim that Windows games are playable. The next graphics milestone is a minimal X11 window rendered into the Android surface with a known test client; only after that should DXVK be enabled.
