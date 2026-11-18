#!/usr/bin/env bash
set -euo pipefail
OUT="${1:-reports}"; mkdir -p "$OUT"
python3 - "$OUT" <<'PY'
import os,re,json,pathlib,sys
out=pathlib.Path(sys.argv[1])
def env(k):return os.environ.get(k) or "TIDAK TERSEDIA"
def read(p):
 try:return pathlib.Path(p).read_text(errors="replace")
 except:return ""
def pick(pat,txt):
 m=re.search(pat,txt,re.I|re.M);return m.group(1).strip() if m else "TIDAK TERSEDIA"
def quote(txt):return "\n".join("> "+line if line else ">" for line in txt.splitlines())
workflow=env("REPORT_WORKFLOW")
slug=re.sub(r"[^a-z0-9]+","-",workflow.lower()).strip("-") or "workflow"
try:jobs=json.loads(env("REPORT_JOB_RESULTS"))
except:jobs={}
states=[v.get("result") if isinstance(v,dict) else v for v in jobs.values()] if isinstance(jobs,dict) else []
result="SUKSES" if states and all(x=="success" for x in states) else "DIBATALKAN" if "cancelled" in states else "GAGAL" if "failure" in states else "TIDAK TERSEDIA"
root=pathlib.Path(env("REPORT_ARTIFACT_DIR") if env("REPORT_ARTIFACT_DIR")!="TIDAK TERSEDIA" else "artifacts")
files=[p for p in root.rglob("*") if p.is_file()] if root.exists() else []
texts={p.name:read(p) for p in files}; alltext="\n".join(texts.values())
a=[]
add=a.append
add("# LAPORAN BUILD — "+workflow+" #"+env("GITHUB_RUN_NUMBER"))
add("**Waktu:** "+env("REPORT_STARTED_AT")+" – "+env("REPORT_COMPLETED_AT")+", durasi total "+env("REPORT_DURATION"))
add("**Repo/Branch:** "+env("GITHUB_REPOSITORY")+" / "+env("GITHUB_REF_NAME"))
add("**Commit:** "+env("GITHUB_SHA")+" — "+env("REPORT_COMMIT_MESSAGE"))
add("**Run:** https://github.com/"+env("GITHUB_REPOSITORY")+"/actions/runs/"+env("GITHUB_RUN_ID")+" | **Pemicu:** "+env("GITHUB_EVENT_NAME")+" | **Runner:** OS "+pick(r"runner_os=(\S+)",alltext)+", arch "+pick(r"uname_m=(\S+)",alltext)+", nproc "+pick(r"nproc=(\S+)",alltext)+", RAM "+pick(r"Mem:\s+(\S+)",alltext))
add("**Hasil keseluruhan:** "+result);add("")
add("## 1. Ringkasan tahap (tabel)");add("| Tahap/Job | Status | Durasi | Catatan |");add("|---|---|---|---|")
for k,v in (jobs.items() if isinstance(jobs,dict) else []):
 status=v.get("result","TIDAK TERSEDIA") if isinstance(v,dict) else v
 add(f"| {k} | {status} | TIDAK TERSEDIA | needs result |")
if not jobs:add("| TIDAK TERSEDIA | TIDAK TERSEDIA | TIDAK TERSEDIA | job result tidak dikirim |")
add("");add("## 2. Smoke ladder (khusus runtime)");add("| Rung | Perintah | Exit code | Durasi | Status | Kutipan log kunci (maks 3 baris) |");add("|---|---|---|---|---|")
for rung,cmd in [("a","box64 --version"),("b","box64 wineserver --version"),("c","box64 wine --version"),("d","box64 wineboot"),("e","box64 wine cmd /c ver")]:
 hit=next(((n,t) for n,t in texts.items() if re.match(rung+r"-.*\.log$",n)),None)
 if not hit:add(f"| {rung} | {cmd} | TIDAK TERSEDIA | TIDAK TERSEDIA | NOT RUN / TIDAK TERSEDIA | log tidak ditemukan |");continue
 n,t=hit;ec=pick(r"exit_code=([^\s]+)",t);dur=pick(r"duration_seconds=([^\s]+)",t)
 status="PASS" if ec=="0" else "TIMEOUT" if ec=="124" else "FAIL" if ec!="TIDAK TERSEDIA" else "TIDAK TERSEDIA"
 excerpt="; ".join(x[:150] for x in t.splitlines() if re.search(r"\[RUN\]|\[FAIL\]|SIGSEGV|eglChooseConfig|wine-[0-9]|Box64 arm",x,re.I)][:3]) or "TIDAK TERSEDIA"
 add(f"| {rung} | {cmd} | {ec} | {dur} | {status} | {excerpt.replace('|','/')} |")
add("");add("## 3. Kegagalan (jika ada)");add("- stage/line/exit_code/command: "+pick(r"\[ERROR\].*",alltext));add("- 50 baris log terakhir:")
for line in alltext.splitlines()[-50:]:add("  "+line[:220])
add("- Klasifikasi penyebab: belum diketahui kecuali didukung bukti eksplisit pada log.")
add("");add("## 4. Versi komponen")
for label,pat in [("Box64",r"Box64[^\n]{0,100}"),("Wine",r"wine-[0-9][^\n]*"),("rootfs distro",r"debian_suite=[^\n]+"),("libc6",r"libc6[^\n]+"),("glibc maksimum Box64",r"box64_max_required_glibc=[^\n]+"),("VERSION",r"VERSION=[^\n]+"),("versi manifest",r'"version"\s*:\s*"[^"]+"'),("status manifest",r'"status"\s*:\s*"(pending|active)"')]:add("- "+label+": "+pick(pat,alltext))
add("");add("## 5. Ukuran dan artefak")
for label,pat in [("rootfs sebelum trim",r"rootfs-size-before-trim[^\n]*"),("rootfs sesudah trim",r"rootfs-size-after-trim[^\n]*"),("archive bytes",r"ARCHIVE_BYTES=([0-9]+)")]:add("- "+label+": "+pick(pat,alltext))
for p in files:add("- "+p.relative_to(root).as_posix()+": "+str(p.stat().st_size)+" byte")
add("- APK bytes/delta, SHA-256: "+pick(r"APK_BYTES=([^\s]+)",alltext)+" / "+pick(r"APK_DELTA_BYTES=([^\s]+)",alltext)+" / "+pick(r"SHA256=([^\s]+)",alltext))
add("- EMBEDDED_RUNTIME_PARTS: "+pick(r"EMBEDDED_RUNTIME_PARTS=([^\s]+)",alltext))
add("");add("## 6. Performa build");add("- Durasi per job: TIDAK TERSEDIA bila tidak dicatat. Cache Box64/rootfs/Gradle: "+pick(r"cache[^\n]*(?:hit|miss)[^\n]*",alltext));add("- Perbandingan run sukses sebelumnya: TIDAK TERSEDIA jika baseline tidak tersedia.")
add("");add("## 7. Perubahan sejak run sebelumnya");add("- Commit: "+env("GITHUB_SHA")+" — "+env("REPORT_COMMIT_MESSAGE"));add("- Berkas berubah: TIDAK TERSEDIA jika event/API tidak memasok daftar perubahan.")
add("");add("## 8. Tingkat bukti dan gerbang");add("| Gerbang R/P/D/G | Status | Bukti (CI / emulator / MatePad) |");add("|---|---|---|");add("| R | lihat smoke ladder | CI artifact/log |");add("| P | TIDAK TERSEDIA | runtime archive/installer evidence |");add("| D | TIDAK TERSEDIA | emulator/MatePad evidence tidak dikumpulkan CI |");add("| G | TIDAK TERSEDIA | game evidence tidak dikumpulkan CI |");add("- Belum diuji di MatePad jika belum ada laporan pengguna. GHOST_TOWN_DISPLAY_READY=1 tetap wajib.")
add("");add("## 9. Observasi dan rekomendasi");add("- TIDAK TERSEDIA tidak sama dengan PASS.");add("- Periksa log mentah untuk memastikan akar masalah; report tidak menebak penyebab.");add("- Lanjutkan dari rung/gerbang pertama yang belum lolos.")
add("");add("## 10. Tautan");add("- Run: https://github.com/"+env("GITHUB_REPOSITORY")+"/actions/runs/"+env("GITHUB_RUN_ID"));add("- Commit: https://github.com/"+env("GITHUB_REPOSITORY")+"/commit/"+env("GITHUB_SHA"));add("- Dokumen: docs/runtime-debug.md, docs/build-reports.md, docs/feasibility-spike.md")
body=quote("\n".join(a)+"\n")
path=out/f"build-report-{slug}-{env('GITHUB_RUN_NUMBER')}.md";path.write_text(body+"\n")
summary=os.environ.get("GITHUB_STEP_SUMMARY")
if summary:
 with open(summary,"a") as f:f.write(body+"\n")
print(body)
PY
