#!/usr/bin/env python3
"""Validate the separate Debian/Wine runtime pack before publishing its release asset.

Absolute symlinks in rootfs are interpreted as guest-root paths and must be
rebased by the Android installer. This validator never extracts archive data.
"""
from __future__ import annotations

import json
import posixpath
import sys
import tarfile
from pathlib import Path

REQUIRED = {
    "rootfs/etc/os-release",
    "rootfs/usr/local/bin/box64",
    "rootfs/opt/wine/bin/wine",
    "rootfs/usr/lib/x86_64-linux-gnu/dri/virtio_gpu_dri.so",
    "rootfs/usr/bin/glxinfo",
    "rootfs/usr/bin/xmessage",
    "provenance/RUNTIME-SHA256SUMS",
}


def safe_name(name: str) -> str:
    normalized = posixpath.normpath(name)
    if name.startswith("/") or normalized == ".." or normalized.startswith("../"):
        raise ValueError(f"unsafe archive path: {name!r}")
    return normalized


def validate(path: str) -> None:
    with tarfile.open(path, "r:gz") as archive:
        members = archive.getmembers()
        names: set[str] = set()
        regular_targets: set[str] = set()
        symlinks = hardlinks = 0
        for member in members:
            name = safe_name(member.name)
            names.add(name)
            if member.issym():
                symlinks += 1
                link = member.linkname
                if link.startswith("/"):
                    if not name.startswith("rootfs/"):
                        raise ValueError(f"absolute symlink outside rootfs: {name} -> {link}")
                    target = posixpath.normpath("rootfs/" + link.lstrip("/"))
                else:
                    target = posixpath.normpath(posixpath.join(posixpath.dirname(name), link))
                if target != "rootfs" and not target.startswith("rootfs/"):
                    raise ValueError(f"symlink escapes guest rootfs: {name} -> {link}")
            elif member.islnk():
                hardlinks += 1
                target = safe_name(member.linkname)
                if target not in regular_targets:
                    raise ValueError(f"hardlink target must be a preceding regular file: {name} -> {target}")
                regular_targets.add(name)
            elif member.isfile():
                regular_targets.add(name)
            elif not (member.isdir() or member.isfifo() or member.ischr() or member.isblk()):
                raise ValueError(f"unsupported tar entry type: {name}")

        missing = sorted(REQUIRED - names)
        if missing:
            raise ValueError("runtime archive missing required paths: " + ", ".join(missing))
        if not symlinks:
            raise ValueError("runtime archive unexpectedly contains no symlinks")

        dri_prefix = "rootfs/usr/lib/x86_64-linux-gnu/dri/"
        dri_files = sorted(name for name in names if name.startswith(dri_prefix) and name.endswith(".so"))
        manifest_path = Path(__file__).resolve().parents[1] / "runtime" / "manifest.json"
        expected = json.loads(manifest_path.read_text(encoding="utf-8"))["components"]["virgl"]["driver"]
        expected_member = "rootfs" + expected if expected.startswith("/") else expected
        print("Mesa DRI inventory:")
        for name in dri_files:
            print(f"  {name}")
        if not dri_files:
            print("WARNING: no Mesa DRI shared drivers found; graphics validation remains pending.")
        if expected_member not in names:
            print(f"WARNING: manifest expected driver missing from archive: {expected_member}")
        print(f"Runtime archive OK: {len(members)} entries, {symlinks} symlinks, {hardlinks} hardlinks")


if __name__ == "__main__":
    try:
        validate(sys.argv[1])
    except Exception as exc:  # CI must print the precise archive validation failure.
        print(f"RUNTIME ARCHIVE VALIDATION FAILED: {exc}", file=sys.stderr)
        raise SystemExit(1)
