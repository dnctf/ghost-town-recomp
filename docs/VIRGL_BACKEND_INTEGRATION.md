# VirGL display backend — implementation direction

## Selected first-render path

For the first end-to-end Wine frame on the Snapdragon 888 / Adreno 660 target, the project is evaluating the **X11 + VirGL + WineD3D** route as the initial rendering backend.

This is intentionally separate from the optional **DXVK + guest Vulkan ICD** route:

- Android host: owns an EGL/GLES context and the Android display surface.
- Host VirGL renderer: translates the guest VirGL command stream into host OpenGL ES work.
- Wine guest: uses a glibc Mesa VirGL driver and WineD3D to translate Direct3D calls to OpenGL.
- X11 server: provides the windowing/display contract expected by Wine's X11 driver and forwards rendered windows to the Android surface.

The initial VirGL route avoids pretending that Android's Bionic Vulkan loader can be loaded directly by the glibc Wine guest. DXVK remains disabled until a separate guest Vulkan ICD and WSI path is proven.

## Source provenance

The host VirGL implementation is pinned to WinlatorXR commit:

`39b270c6401a69e7c28ea6ffd61de3dbac9e9b1e`

Repository: https://github.com/WinlatorXR/WinlatorXR

The repository describes its modified code as GPL-3.0 and upstream code as MIT. Its source is included as a Git submodule; only `app/src/main/cpp/virglrenderer` is built by this project. Nested OpenXR and SDL submodules are deliberately not initialized. Preserve the upstream license and attribution when redistributing builds.

## Current implementation milestone

- [x] Pin the upstream VirGL host renderer source.
- [x] Add its CMake target to the Android ARM64 native build.
- [ ] Confirm `libvirglrenderer.so` builds and is packaged in the APK.
- [ ] Port the minimal X11 server, socket connector, and EGL window renderer.
- [ ] Build/package the guest Mesa `virgl` driver and its runtime dependencies.
- [ ] Start the X11/VirGL services before launching Wine.
- [ ] Verify a harmless X11 test window is visible on the Android surface.
- [ ] Verify a Direct3D 11 smoke-test window through WineD3D.
- [ ] Validate audio/input/lifecycle and then test Overcooked! 2.

## Safety gate

Bundling `libvirglrenderer.so` alone does not create an X11 server or a working display. The game launch gate remains closed until a visible X11 test window and a visible D3D11 smoke-test frame have both been recorded on the target device.
