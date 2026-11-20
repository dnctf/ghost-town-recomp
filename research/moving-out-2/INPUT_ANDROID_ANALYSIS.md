# Moving Out 2 — Input / Android Surface Analysis

## Moving Out 2 evidence

- **VERIFIED (baseline inventory):** no controller-specific dependency or input API report is included.
- **NOT TESTED:** XInput / DirectInput / Raw Input imports, Steam Input use, controller hotplug, keyboard/mouse requirements, local co-op input routing, and input focus behavior.
- **HYPOTHESIS:** couch co-op gameplay makes multi-controller enumeration and stable per-player device identity important, but exact game behavior must be confirmed from legitimate runtime testing.

## Ghost Town source audit

- **VERIFIED:** Android gamepad manager/profile and virtual gamepad overlay source files exist.
- **VERIFIED:** `X11DisplaySession` forwards touch pointer movement and left-button press/release to XServer.
- **NOT TESTED:** physical controller events forwarded into the Wine guest, XInput/DirectInput mapping, multiple controllers, hotplug, dead zones, rumble, keyboard mapping, pointer capture, and input focus.
- **VERIFIED:** AndroidGameSessionHost is currently an interface contract; it is not proof of a connected production surface/input bridge.

## Android lifecycle risks

Activity pause/resume, orientation/size changes, surface destruction/recreation, external display changes, process death, and audio focus can invalidate graphics/input state. A surface token must not be treated as proof that the guest renderer has a valid swapchain.

## Validation plan

Test Android lifecycle independently using a synthetic guest window; verify X11 input events and controller identity; then validate Wine input APIs with a non-game test app. Only after that run a game-level input test. Record each controller and player mapping explicitly.
