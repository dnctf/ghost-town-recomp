#!/usr/bin/env python3
"""Inspect a user-owned Unity Windows game installation; optionally launch via an explicit runner."""
from __future__ import annotations
import argparse, hashlib, json, os, struct, subprocess, sys, time
from pathlib import Path

DATA_MARKERS = ("globalgamemanagers", "data.unity3d", "level0")

def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()

def pe_arch(path: Path) -> str:
    try:
        with path.open("rb") as f:
            if f.read(2) != b"MZ":
                return "NOT_PE"
            f.seek(0x3C)
            raw = f.read(4)
            if len(raw) != 4:
                return "INVALID_PE"
            pe_offset = struct.unpack("<I", raw)[0]
            f.seek(pe_offset)
            if f.read(4) != b"PE\0\0":
                return "INVALID_PE"
            machine = struct.unpack("<H", f.read(2))[0]
        return {0x8664: "x86_64", 0x14C: "x86", 0xAA64: "ARM64"}.get(machine, f"UNKNOWN_0x{machine:04X}")
    except (OSError, struct.error):
        return "UNREADABLE"

def walk_files(root: Path):
    for base, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if d not in (".git", "__pycache__")]
        for name in files:
            yield Path(base) / name

def inspect(root: Path) -> dict:
    root = root.resolve()
    files = list(walk_files(root))
    by_lower = {}
    for p in files:
        by_lower.setdefault(p.name.lower(), []).append(p)
    executables = [p for p in files if p.suffix.lower() == ".exe"]
    data_dirs = []
    for p in root.rglob("*"):
        if p.is_dir() and (p.name.lower().endswith("_data") or p.name.lower() in ("data",)):
            if any((p / marker).is_file() for marker in DATA_MARKERS):
                data_dirs.append(p)
    unity_players = by_lower.get("unityplayer.dll", [])
    results = []
    for exe in executables:
        exe_dir = exe.parent
        matching_data = [d for d in data_dirs if d.parent == exe_dir]
        if not matching_data:
            matching_data = [d for d in data_dirs if exe.stem.lower() in d.name.lower().replace("_data", "")]
        data = matching_data[0] if matching_data else None
        player = next((p for p in unity_players if p.parent == exe_dir), None)
        mono = (data / "Managed" / "Assembly-CSharp.dll") if data else None
        il2cpp = (data / "il2cpp_data" / "Metadata" / "global-metadata.dat") if data else None
        gameassembly = next((p for p in files if p.name.lower() == "gameassembly.dll" and p.parent == exe_dir), None)
        backend = "MONO" if mono and mono.is_file() else ("IL2CPP" if il2cpp and il2cpp.is_file() else "UNKNOWN")
        required = {"exe": exe.is_file(), "unityplayer": bool(player and player.is_file()),
                    "data_directory": bool(data), "managed_or_il2cpp": bool((mono and mono.is_file()) or (il2cpp and il2cpp.is_file()))}
        complete = all(required.values())
        results.append({
            "executable": str(exe.relative_to(root)), "architecture": pe_arch(exe),
            "unity_player": str(player.relative_to(root)) if player else None,
            "data_directory": str(data.relative_to(root)) if data else None,
            "backend_hint": backend, "gameassembly": str(gameassembly.relative_to(root)) if gameassembly else None,
            "requirements": required,
            "status": "COMPLETE_WINDOWS_INSTALLATION_RUNTIME_NOT_INCLUDED" if complete else "INCOMPLETE_OR_DATA_ONLY"
        })
    return {"schema_version": 1, "root": str(root), "executable_count": len(executables),
            "installations": results,
            "summary": "Found executable candidates; runtime compatibility still requires a real launch test." if results
                       else "No Windows executable found. A Unity _Data folder alone is not a runnable PC installation."}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("folder", type=Path)
    parser.add_argument("--json", dest="json_path", type=Path)
    parser.add_argument("--run", action="store_true", help="Actually start the selected EXE with an explicitly supplied runner.")
    parser.add_argument("--runner", nargs="+", help="Runner command prefix, e.g. box64 /path/to/wine. Never invoked unless --run is set.")
    parser.add_argument("--exe", help="Executable relative path to launch when multiple EXEs are found.")
    parser.add_argument("--timeout", type=int, default=90)
    args = parser.parse_args()
    if not args.folder.is_dir():
        parser.error("folder must be an existing directory")
    report = inspect(args.folder)
    rendered = json.dumps(report, indent=2)
    print(rendered)
    if args.json_path:
        args.json_path.write_text(rendered + "\n", encoding="utf-8")
    if not args.run:
        return 0 if report["installations"] else 2
    if not args.runner:
        parser.error("--run requires --runner; no shell command is inferred")
    candidates = report["installations"]
    chosen = next((x for x in candidates if x["executable"] == args.exe), None) if args.exe else (candidates[0] if len(candidates) == 1 else None)
    if not chosen:
        parser.error("select exactly one executable with --exe when multiple candidates exist")
    exe = args.folder.resolve() / chosen["executable"]
    if not exe.is_file():
        parser.error("selected executable disappeared")
    command = args.runner + [str(exe)]
    print("RUNNER:", json.dumps(command))
    started = time.monotonic()
    try:
        completed = subprocess.run(command, cwd=str(exe.parent), timeout=args.timeout, check=False)
        print(json.dumps({"launch_attempted": True, "exit_code": completed.returncode,
                          "elapsed_seconds": round(time.monotonic()-started, 2)}))
        return completed.returncode
    except subprocess.TimeoutExpired:
        print(json.dumps({"launch_attempted": True, "timed_out": True, "timeout_seconds": args.timeout}))
        return 124
    except OSError as exc:
        print(json.dumps({"launch_attempted": True, "error": str(exc)}), file=sys.stderr)
        return 127

if __name__ == "__main__":
    raise SystemExit(main())
