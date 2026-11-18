#!/usr/bin/env python3
"""Summarize an assembled runtime tree by top-level component and largest files."""
import os
import sys

root = os.path.abspath(sys.argv[1])
items = []
groups = {}
for base, dirs, files in os.walk(root, followlinks=False):
    dirs[:] = [d for d in dirs if not os.path.islink(os.path.join(base, d))]
    for name in files:
        p = os.path.join(base, name)
        if os.path.islink(p):
            size = len(os.readlink(p))
        else:
            try:
                size = os.path.getsize(p)
            except OSError:
                continue
        rel = os.path.relpath(p, root)
        top = rel.split(os.sep, 1)[0]
        groups[top] = groups.get(top, 0) + size
        items.append((size, rel))
total = sum(x[0] for x in items)
print(f"RUNTIME_ROOT={root}")
print(f"file_count={len(items)}")
print(f"logical_file_bytes={total}")
print("\nTOP 50 FILES")
for size, rel in sorted(items, reverse=True)[:50]:
    print(f"{size}\t{rel}")
print("\nTOP-LEVEL COMPONENTS")
for name, size in sorted(groups.items(), key=lambda x: x[1], reverse=True):
    print(f"{name}\t{size}\t{(size*100/total if total else 0):.2f}%")
