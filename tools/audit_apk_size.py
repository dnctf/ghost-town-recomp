#!/usr/bin/env python3
"""Report APK ZIP entry sizes without modifying the input."""
import collections
import os
import sys
import zipfile

path = sys.argv[1]
with zipfile.ZipFile(path) as zf:
    entries = [i for i in zf.infolist() if not i.is_dir()]
    total_u = sum(i.file_size for i in entries)
    total_c = sum(i.compress_size for i in entries)
    print(f"APK={path}")
    print(f"entries={len(entries)}")
    print(f"archive_bytes={os.path.getsize(path)}")
    print(f"entry_uncompressed_bytes={total_u}")
    print(f"entry_compressed_bytes={total_c}")
    print("\nTOP 50 BY UNCOMPRESSED SIZE")
    print("uncompressed_bytes\tcompressed_bytes\tmethod\tpath")
    for i in sorted(entries, key=lambda x: x.file_size, reverse=True)[:50]:
        print(f"{i.file_size}\t{i.compress_size}\t{i.compress_type}\t{i.filename}")
    groups = collections.defaultdict(lambda: [0, 0, 0])
    for i in entries:
        p = i.filename.split("/")
        key = p[0] + "/" if len(p) > 1 else p[0]
        groups[key][0] += i.file_size
        groups[key][1] += i.compress_size
        groups[key][2] += 1
    print("\nDIRECTORY SUMMARY")
    print("path\tuncompressed_bytes\tcompressed_bytes\tentries\tshare_uncompressed_pct")
    for key, (u, c, n) in sorted(groups.items(), key=lambda x: x[1][0], reverse=True):
        pct = (u * 100 / total_u) if total_u else 0
        print(f"{key}\t{u}\t{c}\t{n}\t{pct:.2f}")
    for key in ("lib/", "assets/", "res/"):
        u, c, n = groups.get(key, (0, 0, 0))
        print(f"SPECIAL {key} uncompressed={u} compressed={c} entries={n}")
    dex = [i for i in entries if i.filename.endswith(".dex")]
    print("DEX uncompressed_bytes=" + str(sum(i.file_size for i in dex)))
    print("DEX compressed_bytes=" + str(sum(i.compress_size for i in dex)))
    required_native = {
        "lib/arm64-v8a/libproot.so",
        "lib/arm64-v8a/libproot-loader.so",
        "lib/arm64-v8a/libvirglrenderer.so",
    }
    present = {i.filename for i in entries}
    missing = sorted(required_native - present)
    if missing:
        raise SystemExit("Missing W^X-safe ARM64 native payload(s): " + ", ".join(missing))
    embedded_runtime = [i.filename for i in entries if i.filename.startswith("assets/runtime/parts/")]
    if embedded_runtime:
        raise SystemExit("Runtime payload must not be embedded in the base APK: " + ", ".join(embedded_runtime[:5]))
    print("W_X_SAFE_PROOT_NATIVE_LIBS=present")
    print("EMBEDDED_RUNTIME_PARTS=0")
