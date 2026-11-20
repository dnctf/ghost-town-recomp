# IL2CPP Metadata & Unity Resource Addendum

**Input set:** user-supplied files received 2026-11-20.  
**Handling:** analyzed locally; none of the binary files are included in this repository.  
**Scope:** structural identification and metadata-string reconnaissance. This is not a complete IL2CPP type/method reconstruction and does not prove runtime compatibility.

## 1. Executive findings

| Finding | Evidence | Status |
|---|---|---|
| `global-metadata.dat` is a structurally plausible IL2CPP metadata file | 15,397,704 bytes; first DWORD `0xFAB11BAF`; metadata version DWORD `29`; 256-byte header | **VERIFIED** |
| The metadata string table contains extensive Moving Out 2 namespaces and API names | String table at file offset 912,968, size 2,404,920 bytes; 130,250 printable ASCII runs extracted from that region | **VERIFIED** |
| The title uses Photon Fusion networking components | Metadata strings include `Fusion.INetworkRunnerCallbacks`, `Fusion.INetworkSceneManager`, `MovingOut2.Network.Fusion`, and related callbacks | **VERIFIED (metadata evidence only)** |
| Steam and Team17 CrossPlay integrations are represented | Assembly-name strings include `MO2.Platforms.Steam.Runtime.dll`, `Team17.CrossPlay.*.dll`, `Team17.Platform.*.Steam.dll`, `com.rlabrecque.steamworks.net.dll` | **VERIFIED (metadata evidence only)** |
| Unity Input System and controller-specific code are represented | `Unity.InputSystem.dll`, `InputAction`, `Gamepad`, `DualShockGamepad`, Steam Input API strings | **VERIFIED (metadata evidence only)** |
| FMOD and Addressables are represented | `FMODUnity.dll`, FMOD bank-loader identifiers, `Unity.Addressables.dll`, Addressables bootstrap/catalog identifiers | **VERIFIED (metadata evidence only)** |
| A Unity resource-version discrepancy exists in the supplied pair | `unity default resources` contains `2021.3.13f1`; `unity_builtin_extra` contains `2021.3.16f1` | **VERIFIED; significance unresolved** |

The new metadata file resolves the earlier inventory gap: the prior baseline did not include it, but the current user upload does. The old statement that class mapping was blocked by the absence of metadata is therefore superseded. A correct version-29 parser and the matching IL2CPP binary are still required for a real class/method map.

## 2. File fingerprints

SHA-256 values are recorded for provenance and repeatability only. They do not establish the source installation or ownership.

| File | Size (bytes) | SHA-256 |
|---|---:|---|
| `global-metadata.dat` | 15,397,704 | `2a3f9e0474622cbc14df7df0885b789610399c269840887b7275d66eda59848a` |
| `unity default resources` | 4,832,152 | `b9fa3d6bc9e3bb89e2be542b00a644971aedeab200ab86bee4354cc4efba848b` |
| `unity_builtin_extra` | 368,480 | `dd34b48f7bc83dd48bf48a2244a15dd82332a70f3dbe2b46fd87695172292a3` |
| `mscorlib.dll-resources.dat` | 337,563 | `c5baa176a5b72cd545266340e42102d393a5e43d38c95796bc828918bb95277f` |
| `Newtonsoft.Json.dll-resources.dat` | 900 | `531de49e73ddea25554c9f656f54da2a665946f3fec57048e0cfe3b9e49b03f8` |
| `System.Data.dll-resources.dat` | 93,718 | `98a7950ec6de3242c2e190dfa630188dfa32511a125542b94baa952d1c757d49` |
| `System.Drawing.Common.dll-resources.dat` | 11,562 | `452f6132bd531d0c15412d3942ad548edefe8b59c48b4debe962cdd93c8af03b` |
| `System.Drawing.dll-resources.dat` | 25,453 | `7c33aa707bc3fdc83991b81a97d6fc622b140762a767300f08f616c2c0a91255` |

## 3. IL2CPP metadata header

Observed first 8 bytes:

- Sanity: `0xFAB11BAF`
- Version: `29`

The first header ranges include:

| Header range | Offset | Size |
|---|---:|---:|
| String literal records | 256 | 186,712 |
| String literal data | 186,968 | 726,000 |
| Metadata string table | 912,968 | 2,404,920 |

All inspected ranges remain within the 15,397,704-byte file. This is a structural sanity check, not full validation of every metadata table or index.

The repository's earlier `Il2CppMetadataLoader` implementation only checked the magic and estimated a method count by dividing a byte count by 32. That is not a valid version-aware metadata parser. It must not be used to publish type counts, method counts, class names, or callable method offsets.

### Parser requirements

1. Implement a version-specific metadata header for version 29.
2. Validate every offset/size pair against file length and expected alignment.
3. Decode string-literal records separately from the metadata string table.
4. Parse images, assemblies, type definitions, methods, fields, parameters, generic containers, and usage tables using the exact version-29 layouts.
5. Cross-reference metadata tokens/RVAs against the matching `GameAssembly.dll` before claiming method addresses.
6. Keep parser output local or export only non-proprietary summaries; do not commit the metadata dump.

## 4. Assembly names observed in metadata

The metadata string table contains these relevant assembly-name strings (presence in metadata does not prove every assembly was loaded or that its native dependency is present):

### Game and studio assemblies

- `Assembly-CSharp.dll`
- `MO2.Client.Runtime.dll`
- `MO2.Core.Runtime.dll`
- `MO2.Development.Runtime.dll`
- `MO2.Extensions.Runtime.dll`
- `MO2.Game.Runtime.dll`
- `MO2.Platforms.Steam.Runtime.dll`
- `MO2.Server.Runtime.dll`
- `MO2.UI.Runtime.dll`
- `SMG.Core.Runtime.dll`
- `SMG.DevMenu.Runtime.dll`
- `SMG.InputLayers.Runtime.dll`
- `SMG.InputSystemExtensions.Runtime.dll`
- `SMG.LocalDev.Runtime.dll`
- `SMG.Logging.dll`
- `SMG.ScreenRecording.dll`
- `SMG.Serialization.dll`
- `SMG.StackFsm.Runtime.dll`
- `SMG.UI.Runtime.dll`

### Team17 platform and cross-play

- `Team17.CrossPlay.dll`
- `Team17.CrossPlay.Auth.dll`
- `Team17.CrossPlay.Auth.Platform.Steam.dll`
- `Team17.CrossPlay.ConnectionModeSwitcher.dll`
- `Team17.CrossPlay.Core.Platform.dll`
- `Team17.CrossPlay.Core.Standalone.dll`
- `Team17.CrossPlay.Core.dll`
- `Team17.CrossPlay.Overlay.Core.dll`
- `Team17.CrossPlay.Overlay.dll`
- `Team17.CrossPlay.Platform.Steam.dll`
- `Team17.Platform.Auth.Core.dll`
- `Team17.Platform.Auth.Steam.dll`
- `Team17.Platform.OnlineUserService.Core.dll`
- `Team17.Platform.OnlineUserService.Steam.dll`
- `Team17.Platform.Sessions.Core.dll`
- `Team17.Platform.Sessions.Steam.dll`
- `Team17.Platform.User.Steam.dll`
- `Team17.Platform.User.dll`
- `Team17.Sessions.Game.Steam.dll`
- `com.rlabrecque.steamworks.net.dll`

### Runtime and middleware

- `Fusion.Addressables.dll`
- `Fusion.Common.dll`
- `Fusion.Realtime.dll`
- `Fusion.Runtime.dll`
- `Fusion.Sockets.dll`
- `Photon3Unity3D.dll`
- `FMODUnity.dll`
- `FMODUnityResonance.dll`
- `Unity.Addressables.dll`
- `Unity.ResourceManager.dll`
- `Unity.InputSystem.dll`
- `Unity.Localization.dll`
- `Unity.RenderPipelines.Core.Runtime.dll`
- `Unity.RenderPipelines.Universal.Runtime.dll`
- `Unity.RenderPipeline.Universal.ShaderLibrary.dll`
- `Unity.TextMeshPro.dll`
- `Unity.Timeline.dll`
- `Unity.Burst.dll`
- `Unity.Mathematics.dll`
- `UniTask.dll`
- `UniTask.Addressables.dll`
- `websocket-sharp.dll`
- `NanoSockets.dll`

## 5. Game-specific metadata evidence

The string table contains a broad namespace layout, including:

- `MovingOut2.Network`, `MovingOut2.Network.States`, `MovingOut2.Network.Services`, `MovingOut2.Network.Fusion`, `MovingOut2.Network.Data`, `MovingOut2.Network.Behaviours`, `MovingOut2.Network.Auth`
- `MovingOut2.Movers`, `MovingOut2.Movers.Drivers`, `MovingOut2.Movers.Appearance`, `MovingOut2.Movers.Abilities`
- `MovingOut2.Levels`, `MovingOut2.Levels.Systems`, `MovingOut2.Levels.Physics`, `MovingOut2.Levels.Objects`, `MovingOut2.Levels.Logic`, `MovingOut2.Levels.Cutscenes`
- `MovingOut2.Overworld`, `MovingOut2.Lobby`, `MovingOut2.Input`, `MovingOut2.Haptics`, `MovingOut2.CharacterSelect`, `MovingOut2.Campaigns`, `MovingOut2.Data`, `MovingOut2.UI.Dialogue`

Notable Fusion callback/interface identifiers include:

- `Fusion.INetworkRunnerCallbacks.OnPlayerJoined`
- `Fusion.INetworkRunnerCallbacks.OnPlayerLeft`
- `Fusion.INetworkRunnerCallbacks.OnInput`
- `Fusion.INetworkRunnerCallbacks.OnShutdown`
- `Fusion.INetworkRunnerCallbacks.OnConnectedToServer`
- `Fusion.INetworkRunnerCallbacks.OnConnectFailed`
- `Fusion.INetworkRunnerCallbacks.OnDisconnectedFromServer`
- `Fusion.INetworkRunnerCallbacks.OnSessionListUpdated`
- `Fusion.INetworkRunnerCallbacks.OnSceneLoadStart`
- `Fusion.INetworkRunnerCallbacks.OnSceneLoadDone`
- `Fusion.INetworkSceneManager.Initialize`
- `Fusion.INetworkSceneManager.Shutdown`
- `Fusion.IAfterTick.AfterTick`, `Fusion.IBeforeTick.BeforeTick`, `Fusion.IAfterAllTicks.AfterAllTicks`

Input-related identifiers include `InputAction`, `Gamepad`, `DualShockGamepad`, `DualShock4GamepadHID`, `SteamInput`, `GetISteamInput`, and Moving Out-specific `TruckInputAction` / `MoverInputAction` names.

Audio-related identifiers include `FMODBankGroup`, `FMODBanks`, `ClientLevelFMODBankLoader`, `CustomFMODBankLoader`, and `m_PreloadFMODBanks`.

Addressables-related identifiers include `BootstrapFromAddressablesPrefab`, `AddressablesServiceInitializer`, `LoadAllCatalogsAsync`, and catalog-related identifiers.

These are metadata names only. They do not establish actual service endpoints, credentials, online availability, successful platform initialization, or runtime code paths.

## 6. Unity resource files

### Version markers

- `unity default resources`: printable version marker `2021.3.13f1`
- `unity_builtin_extra`: printable version marker `2021.3.16f1`

The second marker matches the earlier baseline Unity 2021.3.16f1 lead. The first is older by three patch releases. This could reflect a stale/shared resource, a mixed extraction, or an artifact from a different source package. Do not silently normalize the discrepancy: verify both files against the exact game installation and record their original paths before using them as authoritative engine-version evidence.

The resource files contain Unity built-in/editor resource identifiers (for example UIElements/VisualTreeAsset/StyleSheet strings and shader property names). This string inspection does not decode serialized Unity objects or identify the game's active render pipeline conclusively.

### DLL resource sidecars

The supplied `*.dll-resources.dat` files are resource payloads, not the corresponding managed DLL binaries. Examples:

- `mscorlib.dll-resources.dat` exposes collation resource names such as `collation.core.bin` and language-specific collation data.
- `Newtonsoft.Json.dll-resources.dat` contains a linker descriptor (`Newtonsoft.Json.xml`) with preserve rules.
- `System.Data.dll-resources.dat` contains embedded SQL metadata/XML resource text.
- `System.Drawing.Common.dll-resources.dat` contains resource identifiers and localized error-message strings.
- `System.Drawing.dll-resources.dat` includes icon resource names such as `Mono.ico`, `Information.ico`, and `Error.ico`.

These sidecars do not substitute for the corresponding assemblies and do not demonstrate that System.Drawing or System.Data are used by gameplay at runtime.

## 7. Updated compatibility implications

| Area | Updated assessment | Engineering consequence |
|---|---|---|
| IL2CPP metadata | Metadata version 29 is now available | Replace the simplistic loader with a versioned parser; obtain matching `GameAssembly.dll` for cross-reference |
| Networking | Fusion/Photon integration is strongly represented | Prioritize offline/local startup path and identify whether network services are mandatory before designing stubs; do not fake online auth |
| Platform services | Steam and Team17 cross-play layers are represented | Define a platform abstraction and explicit unsupported/standalone mode only where the game permits it |
| Input | Unity Input System, gamepad, DualShock and Steam Input identifiers are present | Map Android controller events to guest input; test player-join/device-change flows and haptics separately |
| Audio | FMOD and bank-loading paths are represented | Plan FMOD native dependency and audio-device compatibility; sidecar resources are not the FMOD runtime |
| Content loading | Addressables/catalog/bootstrap identifiers are present | Verify catalog and bundle layout from legitimate local installation; ensure file paths/case/async loading semantics |
| Graphics | URP assemblies and shader-related identifiers appear in metadata | This supports URP presence in the managed metadata, but does not prove the active renderer, shader variants, or Vulkan compatibility |
| Engine version | Built-in resource version markers disagree | Reconcile exact provenance before freezing engine compatibility assumptions |

## 8. Next experiments (ordered)

1. Obtain or verify the matching `GameAssembly.dll` and PE text report (headers, imports, delay imports, exports, architecture, section map). Do not upload proprietary binaries to GitHub.
2. Run a proper IL2CPP version-29 parser locally and validate assembly/image/type/method counts against table bounds.
3. Generate a text-only assembly/type index grouped by image and namespace; retain method signatures only where parser output is validated.
4. Inspect Addressables catalog and bundle filenames from the local install without committing asset contents.
5. Identify the actual Fusion startup/session mode and determine whether an offline path exists through documented configuration and runtime observation.
6. Trace Input System device registration and platform gamepad scheme selection; test Android HID mappings separately.
7. Inspect FMOD native library names/architectures and bank-loading order; test audio only after process startup is stable.
8. Reconcile the 2021.3.13f1 vs 2021.3.16f1 resource markers with original file paths and installation provenance.

## 9. Explicit non-claims

This addendum does **not** claim:

- complete IL2CPP class/method reconstruction;
- that any network service can be bypassed or that online play works;
- that Steam, Photon/Fusion, FMOD, Addressables, URP, or controller code initializes successfully under Wine/Box64;
- that the game starts, renders, produces audio, or is playable on Android;
- that the supplied Unity resources are all from one exact game build.

No supplied binary or asset is stored in this repository.
