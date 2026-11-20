# Moving Out 2 — Networking / Platform Dependencies

## Evidence

- **VERIFIED (baseline inventory):** `nanosockets.dll`, `steam_api64.dll`, and a Steam-related `.rne` file are listed.
- **INFERRED:** NanoSockets is a networking transport dependency; Steam API provides platform integration. Presence does not prove mandatory startup dependency, online-only behavior, matchmaking implementation, or that the game cannot run offline.
- **NOT TESTED:** imported functions, runtime module load, socket endpoints, Steam initialization behavior, lobby/matchmaking flows, relay use, firewall behavior, or offline startup.
- **NOT TESTED:** no packet capture or runtime trace was performed.

## Ghost Town status

The manifest describes the app as BYOG and states that proprietary game files are not distributed. Wine/Box64 headless bootstrap is separate from game networking. There is no evidence that Steam platform integration has been validated in the Android runtime.

## Safe research boundary

Do not inspect, copy, or publish credentials, account tokens, sensitive Steam configuration, or authentication material. Do not bypass DRM, licensing, ownership checks, or platform authentication. Research only documented API dependencies and normal user-authorized behavior.

## Validation

Use a legitimate Windows installation and normal account/session. Compare an offline launch with a normal online launch using sanitized logs. Record only module-load outcomes, high-level error codes, and network destination categories; omit secrets and personal identifiers.
