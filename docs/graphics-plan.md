# Graphics transport risk register (planning only)

## Current evidence

- The pinned Debian Bookworm amd64 `libgl1-mesa-dri` package exposes `virtio_gpu_dri.so` in its DRI directory. This is the observed package payload name; it is not evidence that this driver is suitable for the Android host transport.
- Mesa documents VirGL as a guest Gallium driver for a virtual 3D GPU, conventionally paired with a host-side virglrenderer implementation. The conventional virtio-gpu route communicates guest GL commands to a host virglrenderer; see [Mesa VirGL documentation](https://docs.mesa3d.org/drivers/virgl.html) and [QEMU virtio-gpu/virgl documentation](https://www.qemu.org/docs/master/system/devices/virtio/virtio-gpu.html).
- Mesa's vtest/virpipe path is a different transport: the guest-side Gallium driver sends commands over a socket to a vtest server. Mesa's original vtest driver commit describes `GALLIUM_DRIVER=virpipe` and socket transport ([Mesa commit](https://cgit.freedesktop.org/mesa/mesa/commit/?id=b3b82fe8ea1f7d02c93513920143cba2fe145b6c)); virglrenderer publishes the vtest protocol and socket implementation ([protocol header](https://android.googlesource.com/platform/external/virglrenderer/+/68429e8e1106d0861d9f9f180583bd8381b8bf96/vtest/vtest_protocol.h)). An Android-focused implementation documents a vtest server on the Android host with Mesa `virpipe` in the guest ([architecture example](https://github.com/DroidNative/DroidNativeApp/blob/main/docs/architecture.md)).
- Therefore, `virtio_gpu_dri.so` (the DRM/virtio-gpu-oriented Mesa driver path) is **not proven** to work with this app's Android GLES socket renderer (vtest/virpipe). The driver may expect a virtio-gpu DRM device rather than the socket transport expected by virpipe. This is a compatibility risk, not a confirmed incompatibility.

## Required device verification

Status: **UNPROVEN — do not mark display ready**.

On the target Android/HarmonyOS device, after the host vtest/virglrenderer socket is started:
1. Record `ls -l` of the guest DRI directory and the selected Gallium driver.
2. Run `glxinfo -B` with the intended `GALLIUM_DRIVER`/ `LIBGL` environment and verify renderer/vendor/version.
3. Confirm the client actually connects to the expected Unix socket (capture server/client logs).
4. Open a minimal X11 GL window and verify a visible frame; then test WineD3D.
5. Keep `GHOST_TOWN_DISPLAY_READY=1` mandatory until all display checks pass.

## Fallback plan — no implementation in this change

If Debian's stock Mesa package does not provide the correct virpipe client or the target test fails:
- Build a separate pinned Mesa guest userspace with the required Gallium `virpipe` driver enabled (and only the needed DRI/Xlib components), using a compatible Mesa/virglrenderer protocol revision.
- Build or package the matching Android host virglrenderer vtest server separately, pin its source commit and build flags, and verify the socket protocol and GLES backend together.
- Record exact Mesa/virglrenderer commits, build options, licenses, ABI, driver inventory and SHA-256 provenance.
- Keep the Debian `virtio_gpu_dri.so` package as a diagnostic/reference option until the alternative passes `glxinfo -B` and visible-frame tests on the device.
- Do not add DXVK, Turnip, or change the rendering mechanism as part of this plan.

No Mesa or virglrenderer rebuild is performed by this document.
