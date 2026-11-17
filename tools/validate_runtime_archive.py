#!/usr/bin/env python3
"""Validate the bundled Debian/Wine runtime tarball before APK packaging.

Absolute symlinks in rootfs are interpreted as guest-root paths and must be
rebased by the Android installer. This validator never extracts archive data.
"""
from __future__ import annotations

import posixpath
import sys
import tarfile
from pathlib import PurePosixPath

REQUIRED = {
    "bin/proot",
    "libexec/proot/loader",
    "rootfs/etc/os-release",
    "rootfs/usr/local/bin/box64",
    "rootfs/opt/wine/bin/wine",
    "rootfs/opt/dxvk/x64/d3d11.dll",
    "rootfs/opt/dxvk/x64/dxgi.dll",
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
                if target not in names:
                    raise ValueError(f"hardlink target must precede link: {name} -> {target}")
            elif not (member.isdir() or member.isfile() or member.isfifo() or member.ischr() or member.isblk()):
                raise ValueError(f"unsupported tar entry type: {name}")

        missing = sorted(REQUIRED - names)
        if missing:
            raise ValueError("runtime archive missing required paths: " + ", ".join(missing))
        if not symlinks:
            raise ValueError("runtime archive unexpectedly contains no symlinks")
        print(f"Runtime archive OK: {len(members)} entries, {symlinks} symlinks, {hardlinks} hardlinks")


if __name__ == "__main__":
    try:
        validate(sys.argv[1])
    except Exception as exc:  # CI must print the precise archive validation failure.
        print(f"RUNTIME ARCHIVE VALIDATION FAILED: {exc}", file=sys.stderr)
        raise SystemExit(1)
