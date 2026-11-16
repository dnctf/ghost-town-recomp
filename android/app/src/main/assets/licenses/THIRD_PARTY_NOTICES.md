# Third-party notices

This application includes or can install the following third-party components. Runtime components are downloaded separately as a user-installed runtime bundle; they are not embedded in the APK.

## Android APK dependencies

- Apache Commons Compress 1.27.1 — Apache License 2.0. Used to stream and extract the runtime `.tar.gz` archive. The Apache license text and Commons Compress NOTICE are in this directory.
- Transitive Apache Commons libraries, if resolved by Gradle — Apache License 2.0. Their exact resolved versions are recorded in the Gradle dependency graph.

## Optional Windows runtime bundle

- PRoot / `berilevi/proot-static` — GPL-2.0-or-later. The upstream COPYING file is included in the runtime artifact.
- Box64 — MIT. See `runtime/licenses/BOX64-LICENSE`.
- Wine 11.19 — GNU LGPL 2.1 or later. See `runtime/licenses/WINE-LICENSE`, `WINE-COPYING.LIB`, and `WINE-NOTICES.md`.
- DXVK 3.1.1 — zlib/libpng license. See `runtime/licenses/DXVK-LICENSE`.
- Debian Bookworm ARM64/amd64 packages — each package's copyright and license notice is retained under `rootfs/usr/share/doc/*/copyright` in the runtime bundle.

The upstream sources and exact refs/checksums are recorded in `runtime/manifest.json` and the runtime artifact's `provenance/` directory. Do not remove license notices when redistributing a runtime bundle.
