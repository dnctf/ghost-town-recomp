# Moving Out 2 — File Inventory

Evidence labels apply to the supplied baseline inventory, not to a fresh filesystem scan.

## Available inventory (baseline record)

| File | Approx. size | SHA-256 | Classification |
|---|---:|---|---|
| mo2.exe | 639 KiB | 032a97e36ddead78ca45007013088818198f18c28f94747dd1431a78291339b0 | VERIFIED in prior analysis session |
| GameAssembly.dll | 76 MiB | 2b97f2108b7cfe300a797ec1d6f12ba7695697871e2489020ffc1d583f4d0b23 | VERIFIED in prior analysis session |
| UnityPlayer.dll | 28 MiB | cd715e67296377198ace9433a0f357056cfb5241da427131f31db3bcb1edab61 | VERIFIED in prior analysis session |
| BuildInfo.json | 170 B | d1329d94e01f015cbd7451a44470014bd20dc2976fa92a887f2c04cd4b6314ad | VERIFIED in prior analysis session |
| baselib.dll | 396 KiB | 46efae8b155e3f4649758b3e23902cd61558079bed47e7caf36b8a4a3d605d33 | VERIFIED in prior analysis session |
| unity default resources | 4.7 MiB | b9fa3d6bc9e3bb89e2be542b00a644971aedeab200ab86bee4354cc4efba848b | VERIFIED in prior analysis session |
| unity_builtin_extra | 360 KiB | dd34bb48f7bc83dd48bf48a2244a15dd82332a70f3dbe2b46fd87695172292a3 | VERIFIED in prior analysis session |
| BacktraceCrashpadWindows.dll | 132 KiB | bc03f282062b1010a30d99a44ffbe97fe9f459298b9754e0a00d98f5b70c6cff | VERIFIED in prior analysis session |
| core_stub.dll | 324 KiB | 62872f6ec39831e9d270f3ad49f773f447237195e9318b761b4699a1504b20e8 | VERIFIED in prior analysis session |
| crashpad_handler.dll | 782 KiB | e1eb08309dd8fb388431304f351b5a41c8740d6542b6390807fdaf43d5edf941 | VERIFIED in prior analysis session |
| fmodstudio.dll | 3.1 MiB | f9ec72222802e8868856becb6000a175203cc60a8b822a7fd9377c757b3a6ba7 | VERIFIED in prior analysis session |
| lib_burst_generated.dll | 73 KiB | 3c123c19e53dd4026f0f1e566023c7b35d6ec7737b65f7dfd1d2543582462b6a | VERIFIED in prior analysis session |
| nanosockets.dll | 95 KiB | 037985bd36c9b6984e8680e9f508491a7056f010bdb7207e32f07aa149eaaf00 | VERIFIED in prior analysis session |
| resonanceaudio.dll | 798 KiB | e1667e5336ae067ab1d9aa04e7bcca9789ff5573ed4c7f6fa9305404d0485f99 | VERIFIED in prior analysis session |
| steam_api64.dll | 417 KiB | 51425709b07d703f2b675828520c8d38af2d014f40d175695bc299c32b37f685 | VERIFIED in prior analysis session |
| steam_api64.rne | 260 KiB | ca1fa9a7609ab10b7926400559cf073e5888423cc156af72c6027d72a89eea73 | VERIFIED in prior analysis session |
| steam_emu.ini | 2.7 KiB | 97c9cb581792fd475592d71c230416a75ebfc7029b6db061fa7b713746af4d48 | VERIFIED in prior analysis session |

The sizes are approximate. The hashes were calculated in the earlier analysis environment; they are identifiers, not proof of provenance.

## Important missing/unknown artifacts

- `global-metadata.dat`: **NOT TESTED / NOT PRESENT IN THE REPOSITORY INVENTORY**. The baseline inventory does not list it. This does not prove it is absent from the user's local game installation.
- `boot.config`, `*_Data/Plugins/x86_64/`, `Player.log`, `UnityPlayer.dll` import/export dumps: not supplied in the baseline record.
- Exact Windows PE dependency closure and delay-load imports: not independently re-run in this continuation because the proprietary binaries are not mounted in this session.

## Next files genuinely useful

1. `global-metadata.dat` — only if the user can provide a local copy for in-session analysis; do not commit it.
2. Text-only PE inspection output for `mo2.exe`, `GameAssembly.dll`, `UnityPlayer.dll`: machine, optional-header magic, imports, delay imports, exports, section summary, compiler/linker metadata.
3. A text-only recursive listing of `*_Data/Plugins/x86_64/` and adjacent `boot.config`.
4. Sanitized `Player.log` from a legitimate Windows launch, if available.

Do not upload or commit executable/DLL/assets. No re-upload of files represented by the hashes above is requested.
