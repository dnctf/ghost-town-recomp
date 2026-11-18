#!/usr/bin/env bash
set -euo pipefail
OUT="${1:-reports}"
mkdir -p "$OUT"
python3 - "$OUT" <<'PY'
import csv, datetime as dt, json, os, pathlib, re, sys
out = pathlib.Path(sys.argv[1])
def env(k, default="TIDAK TERSEDIA"):
    return os.environ.get(k) or default
def load_json(key, fallback=None):
    raw = os.environ.get(key, "")
    if not raw:
        p = os.environ.get(key + "_FILE")
        if p and pathlib.Path(p).exists():
            raw = pathlib.Path(p).read_text(errors="replace")
    try:
        return json.loads(raw) if raw else (fallback if fallback is not None else {})
    except Exception:
        return fallback if fallback is not None else {}
def read_text(p):
    try: return p.read_text(errors="replace")
    except Exception: return ""
def pick(pattern, text, default="TIDAK TERSEDIA"):
    m = re.search(pattern, text, re.I | re.M)
    if not m: return default
    return (m.group(1) if m.lastindex else m.group(0)).strip()
def parse_time(value):
    if not value: return None
    try: return dt.datetime.fromisoformat(value.replace("Z", "+00:00"))
    except Exception: return None
def duration(start, end):
    a, b = parse_time(start), parse_time(end)
    if not a or not b: return "TIDAK TERSEDIA"
    sec = max(0, int((b-a).total_seconds()))
    h, rem = divmod(sec, 3600); m, s = divmod(rem, 60)
    return (f"{h}h " if h else "") + (f"{m}m " if m or h else "") + f"{s}s ({sec}s)"
def redact(s):
    s = re.sub(r"(?i)(gh[pousr]_[A-Za-z0-9_]{20,})", "[REDACTED_TOKEN]", s)
    s = re.sub(r"(?i)(authorization|token|password|secret|api_key)=([^\s]+)", r"\1=[REDACTED]", s)
    s = re.sub(r"(?i)Bearer\s+[^\s]+", "Bearer [REDACTED]", s)
    s = re.sub(r"/home/runner/[^\s:]+", "[RUNNER_PATH]", s)
    s = re.sub(r"/Users/[^\s:]+", "[PRIVATE_PATH]", s)
    return s
def quote(s):
    return "\n".join("> " + line if line else ">" for line in s.splitlines())
workflow = env("REPORT_WORKFLOW")
slug = re.sub(r"[^a-z0-9]+", "-", workflow.lower()).strip("-") or "workflow"
api_status = env("REPORT_API_STATUS", "failed")
run = load_json("REPORT_RUN_JSON")
jobs_api = load_json("REPORT_JOBS_JSON", [])
artifacts_api = load_json("REPORT_ARTIFACTS_JSON", [])
needs = load_json("REPORT_JOB_RESULTS")
if isinstance(jobs_api, dict): jobs_api = jobs_api.get("jobs", [])
if isinstance(artifacts_api, dict): artifacts_api = artifacts_api.get("artifacts", [])
if isinstance(needs, dict):
    needs = [{"name": k, "conclusion": (v or {}).get("result", "TIDAK TERSEDIA")} for k,v in needs.items()]
else: needs = []
root = pathlib.Path(env("REPORT_ARTIFACT_DIR", "artifacts"))
files = sorted(p for p in root.rglob("*") if p.is_file()) if root.exists() else []
texts = {p.relative_to(root).as_posix(): read_text(p) for p in files if p.suffix.lower() in (".log",".txt",".md",".json",".out",".sha256")}
alltext = "\n".join(texts.values())
runtime = bool(re.search(r"runtime|wine", workflow, re.I))
apk_workflow = bool(re.search(r"apk", workflow, re.I))
conclusion = run.get("conclusion") if api_status == "ok" else None
if conclusion == "success": overall = "SUKSES"
elif conclusion in ("cancelled", "canceled"): overall = "DIBATALKAN"
elif conclusion in ("timed_out", "startup_failure"): overall = "TIMEOUT"
elif conclusion == "failure": overall = "GAGAL"
else:
    states = [x.get("conclusion") for x in needs] or [x.get("conclusion") for x in jobs_api]
    overall = "DIBATALKAN" if any(x in ("cancelled","canceled") for x in states) else "TIMEOUT" if "timed_out" in states else "GAGAL" if "failure" in states else "SUKSES" if states and all(x=="success" for x in states) else "TIDAK TERSEDIA"
started = run.get("run_started_at") or run.get("created_at")
if run.get("status") == "completed":
    finished = run.get("updated_at")
else:
    completed = [j.get("completed_at") for j in jobs_api if j.get("completed_at") and "report" not in j.get("name","").lower()]
    finished = max(completed) if completed else None
total_duration = duration(started, finished) if api_status == "ok" else "TIDAK TERSEDIA"
branch = run.get("head_branch") or env("GITHUB_REF_NAME")
sha = run.get("head_sha") or env("GITHUB_SHA")
commit_message = env("REPORT_COMMIT_MESSAGE")
if commit_message == "TIDAK TERSEDIA":
    commit_message = env("GITHUB_EVENT_HEAD_COMMIT_MESSAGE")
runner_os = env("RUNNER_OS")
runner_arch = env("RUNNER_ARCH")
runner_machine = env("REPORT_RUNNER_MACHINE")
runner_nproc = env("REPORT_RUNNER_NPROC")
runner_ram = env("REPORT_RUNNER_RAM")
runner = f"{runner_os}/{runner_arch} ({runner_machine}), nproc={runner_nproc}, RAM={runner_ram}; job runner labels: " + ", ".join(str(x.get("name","")) + " [" + ",".join(x.get("labels",[])) + "]" for x in jobs_api)
manifest = {}
try: manifest = json.loads(pathlib.Path("runtime/manifest.json").read_text())
except Exception: pass
rp = manifest.get("runtime_pack", {}) if isinstance(manifest,dict) else {}
components = manifest.get("components", {}) if isinstance(manifest,dict) else {}
version_file = read_text(pathlib.Path("VERSION")).strip() if pathlib.Path("VERSION").exists() else "TIDAK TERSEDIA"
gradle = read_text(pathlib.Path("android/app/build.gradle")) if pathlib.Path("android/app/build.gradle").exists() else ""
app_version = pick(r'versionName\s+"([^"]+)"', gradle)
box64_commit = components.get("box64",{}).get("source_commit","TIDAK TERSEDIA")
wine_version = components.get("wine",{}).get("version","TIDAK TERSEDIA")
rootfs_distro = components.get("rootfs",{}).get("distribution","TIDAK TERSEDIA")
box64_version = pick(r"Box64 arm64 v[0-9.]+[^\n]*", alltext)
wine_seen = pick(r"wine-[0-9][^\s]*", alltext)
libc = pick(r"rootfs_libc6_package=([^\s]+)", alltext)
glibc = pick(r"box64_max_required_glibc=([^\s]+)", alltext)
if box64_version != "TIDAK TERSEDIA": box64_commit += " / " + box64_version
if wine_seen != "TIDAK TERSEDIA": wine_version += " / CI: " + wine_seen
if not libc: libc = "TIDAK TERSEDIA"
if not glibc: glibc = "TIDAK TERSEDIA"
artifacts_lines = []
for a in artifacts_api:
    artifacts_lines.append(f"- {a.get('name','TIDAK TERSEDIA')}: {a.get('size_in_bytes','TIDAK TERSEDIA')} byte (artifact ZIP)")
if not artifacts_lines:
    artifacts_lines = ["- TIDAK TERSEDIA (API artifact tidak tersedia)"]
def audit_size(folder, name):
    candidates = list(folder.rglob(name)) if folder.exists() else []
    for p in candidates:
        t = read_text(p)
        m = re.search(r"^archive_bytes=(\d+)", t, re.M)
        if m: return int(m.group(1)), t
    return None, ""
apk_release, release_audit = audit_size(root, "app-release.apk.txt")
apk_debug, debug_audit = audit_size(root, "app-debug.apk.txt")
previous_root = pathlib.Path(env("REPORT_PREVIOUS_APK_DIR", "previous-apk"))
prev_release, prev_audit = audit_size(previous_root, "app-release.apk.txt")
prev_run = env("REPORT_PREVIOUS_APK_RUN")
if apk_release is not None and prev_release is not None:
    delta = apk_release - prev_release
    delta_text = f"{delta:+,} byte vs run {prev_run} (current {apk_release:,}; previous {prev_release:,})"
else: delta_text = "TIDAK TERSEDIA (APK audit/baseline sebelumnya tidak ditemukan)"
apk_hash = pick(r"SHA256=([^\s]+)", release_audit)
if apk_hash == "TIDAK TERSEDIA":
    apk_hash = pick(r"([0-9a-f]{64})\s+android/app/build/outputs/apk/release/[^\s]+", release_audit)
apk_embedded = pick(r"EMBEDDED_RUNTIME_PARTS=([^\s]+)", release_audit + "\n" + debug_audit)
if not apk_workflow: apk_status = "N/A (tidak berlaku)"
else: apk_status = f"release={apk_release if apk_release is not None else 'TIDAK TERSEDIA'} byte; debug={apk_debug if apk_debug is not None else 'TIDAK TERSEDIA'} byte; delta={delta_text}"
rungs = [("a","box64 --version"),("b","box64 wineserver --version"),("c","box64 wine --version"),("d","box64 wineboot"),("e","box64 wine cmd /c ver")]
rung_rows=[]
if not runtime:
    rung_rows=["| N/A | N/A (tidak berlaku untuk workflow ini) | N/A | N/A | N/A | N/A |"]
else:
    for rung,cmd in rungs:
        hit = next(((n,t) for n,t in texts.items() if re.search(rf"(^|/){rung}-[^/]*\.log$", n)), None)
        if not hit:
            rung_rows.append(f"| {rung} | {cmd} | TIDAK TERSEDIA | TIDAK TERSEDIA | NOT RUN | Log rung tidak tersedia |")
            continue
        name, txt = hit
        ec = pick(r"^exit_code=([^\s]+)", txt)
        dur = pick(r"^duration_seconds=([^\s]+)", txt)
        state = "PASS" if ec=="0" else "TIMEOUT" if ec in ("124","137") else "FAIL" if ec!="TIDAK TERSEDIA" else "NOT RUN"
        excerpt = [redact(x[:160]) for x in txt.splitlines() if re.search(r"\[RUN\]|\[FAIL\]|SIGSEGV|eglChooseConfig|wine-[0-9]|Box64 arm|\[TIMEOUT\]",x,re.I)][:3]
        rung_rows.append(f"| {rung} | {cmd} | {ec} | {dur} | {state} | {'; '.join(excerpt) if excerpt else 'TIDAK TERSEDIA'} |")
error_lines=[]
for n,txt in texts.items():
    for line in txt.splitlines():
        if "[ERROR]" in line or "Process completed with exit code" in line:
            error_lines.append(redact(line[:350]))
tail=[]
for n,txt in texts.items():
    if n.lower().endswith((".log",".out")):
        tail.extend((n+": "+redact(line[:250])) for line in txt.splitlines()[-50:])
tail=tail[-50:]
job_rows=[]
if api_status=="ok" and jobs_api:
    for j in jobs_api:
        end_at = j.get("completed_at") or env("REPORT_GENERATED_AT")
        job_duration = duration(j.get("started_at"), end_at) if j.get("completed_at") else "IN PROGRESS saat report diambil; elapsed=" + duration(j.get("started_at"), end_at)
        job_rows.append(f"| {j.get('name','TIDAK TERSEDIA')} | {j.get('conclusion') or j.get('status','TIDAK TERSEDIA')} | {job_duration} | runner={j.get('runner_name','TIDAK TERSEDIA')} |")
elif needs:
    for j in needs: job_rows.append(f"| {j.get('name')} | {j.get('conclusion')} | TIDAK TERSEDIA | API job timing tidak tersedia |")
else: job_rows=["| TIDAK TERSEDIA | TIDAK TERSEDIA | TIDAK TERSEDIA | API job tidak tersedia |"]
lines=[]
add=lines.append
add("# LAPORAN BUILD — "+workflow+" #"+env("GITHUB_RUN_NUMBER"))
add(f"**Waktu:** {started if api_status=='ok' and started else 'TIDAK TERSEDIA'} – {finished if api_status=='ok' and finished else 'TIDAK TERSEDIA'}, durasi total {total_duration}")
add(f"**Repo/Branch:** {env('GITHUB_REPOSITORY')} / {branch}")
add(f"**Commit:** {sha} — {commit_message}")
add(f"**Run:** https://github.com/{env('GITHUB_REPOSITORY')}/actions/runs/{env('GITHUB_RUN_ID')} | **Pemicu:** {run.get('event',env('GITHUB_EVENT_NAME')) if api_status=='ok' else env('GITHUB_EVENT_NAME')} | **Runner:** {runner}")
add("**Hasil keseluruhan:** "+overall); add("")
add("## 1. Ringkasan tahap (tabel)"); add("| Tahap/Job | Status | Durasi | Catatan |"); add("|---|---|---|---|")
add("\n".join(job_rows))
add(""); add("## 2. Smoke ladder (khusus runtime)"); add("| Rung | Perintah | Exit code | Durasi | Status | Kutipan log kunci (maks 3 baris) |"); add("|---|---|---|---|---|---|")
add("\n".join(rung_rows))
add(""); add("## 3. Kegagalan (jika ada)")
add("- stage/line/exit_code/command: "+("; ".join(error_lines[:5]) if error_lines else "TIDAK TERSEDIA"))
add("- 50 baris log terakhir (disanitasi):")
add("\n".join("> " + x for x in tail) if tail else "> TIDAK TERSEDIA")
add("- Klasifikasi penyebab: "+("bukti log tersedia; klasifikasi manual diperlukan, jangan menyimpulkan di luar bukti." if error_lines else "belum diketahui / tidak ada log kegagalan yang tersedia."))
add(""); add("## 4. Versi komponen")
add(f"- VERSION: {version_file}; app versionName: {app_version}")
add(f"- Box64 (commit/version): {box64_commit}")
add(f"- Wine (manifest/CI): {wine_version}")
add(f"- Rootfs: {rootfs_distro}; libc6 package: {libc}")
add(f"- glibc maksimum yang diminta Box64: {glibc}")
add(f"- Versi manifest/runtime pack: {rp.get('version','TIDAK TERSEDIA')}")
add(f"- Status manifest runtime_pack: {rp.get('status','TIDAK TERSEDIA')}")
add(""); add("## 5. Ukuran dan artefak")
def named_file_value(basename):
    for p in files:
        if p.name == basename:
            value = read_text(p).strip().splitlines()
            return value[0] if value else "TIDAK TERSEDIA"
    return "TIDAK TERSEDIA"
add(f"- Rootfs sebelum/sesudah trim: {named_file_value('rootfs-size-before-trim.txt')} / {named_file_value('rootfs-size-after-trim.txt')}")
add(f"- Runtime archive bytes: {pick(r'ARCHIVE_BYTES=([0-9]+)',alltext)}; SHA-256: {pick(r'([0-9a-f]{64})\s+ghost-town-wine-runtime-arm64\.tar\.gz',alltext)}; expanded bytes: {pick(r'expanded_size_bytes=([0-9]+)',alltext)}")
add(f"- APK: {apk_status}; SHA-256 release: {apk_hash}; EMBEDDED_RUNTIME_PARTS: {apk_embedded}")
add("\n".join(artifacts_lines))
add(""); add("## 6. Performa build")
add("- Durasi per job berasal dari GitHub Actions jobs API pada bagian 1.")
add(f"- Cache Box64/rootfs/Gradle: {pick(r'(?i)(?:cache[^\n]*(?:hit|miss)|cache hit[^\n]*)',alltext)}")
add(f"- Perbandingan APK: {delta_text}")
add(""); add("## 7. Perubahan sejak run sebelumnya")
add("- Commit: "+sha+" — "+commit_message)
add("- Berkas berubah: "+env("REPORT_CHANGED_FILES"))
add(""); add("## 8. Tingkat bukti dan gerbang")
add("| Gerbang R/P/D/G | Status | Bukti (CI / emulator / MatePad) |"); add("|---|---|---|")
if runtime:
    rung_d=next((x for x in rung_rows if x.startswith("| d |")),"")
    r_status="PASS" if "| PASS |" in rung_d else "FAIL/NOT RUN"
else: r_status="R tidak diuji oleh workflow ini"
add(f"| R | {r_status} | CI log/artifact; bukan bukti perangkat |")
add("| P | TIDAK TERSEDIA | runtime pack/installer hanya jika artifact dan device evidence tersedia |")
add("| D | TIDAK TERSEDIA | visible-frame self-test belum dibuktikan oleh report ini |")
add("| G | TIDAK TERSEDIA | gameplay/audio/input fisik belum dibuktikan oleh report ini |")
add("- Belum diuji di MatePad bila belum ada laporan pengguna. GHOST_TOWN_DISPLAY_READY=1 tetap wajib; laporan tidak membuat marker.")
add(""); add("## 9. Observasi dan rekomendasi")
add("- API GitHub: "+("berhasil" if api_status=="ok" else "gagal; waktu/durasi/job/artifact metadata yang memerlukan API ditulis TIDAK TERSEDIA."))
add("- N/A menandakan bagian tidak relevan; NOT RUN hanya untuk rung runtime yang seharusnya berjalan namun tidak menghasilkan log.")
add("- Tindak lanjut: periksa rung pertama yang belum PASS dan gunakan hanya bukti dari artifact/log.")
add(""); add("## 10. Tautan")
add(f"- Run: https://github.com/{env('GITHUB_REPOSITORY')}/actions/runs/{env('GITHUB_RUN_ID')}")
add(f"- Commit: https://github.com/{env('GITHUB_REPOSITORY')}/commit/{sha}")
add("- Artifact: "+", ".join(a.get("name","TIDAK TERSEDIA") for a in artifacts_api) if artifacts_api else "- Artifact: TIDAK TERSEDIA")
add("- Log mentah: buka run > Jobs > View raw logs; dokumen: docs/runtime-debug.md, docs/build-reports.md, docs/feasibility-spike.md")
body=quote("\n".join(lines)+"\n")
report_path=out/f"build-report-{slug}-{env('GITHUB_RUN_NUMBER')}.md"
report_path.write_text(body+"\n")
summary=os.environ.get("GITHUB_STEP_SUMMARY")
if summary:
    with open(summary,"a") as f: f.write(body+"\n")
print(body)
PY
