#!/usr/bin/env python3
"""Summarize a user-supplied Winlator feasibility table (Markdown or CSV)."""
from __future__ import annotations
import argparse, csv, pathlib, re
ALIASES = {
 "installed":{"installed","install","terpasang","bisa_install","app_installed"},
 "game_visible":{"game_visible","visible","game_tampil","frame_visible","game_frame_visible"},
 "menu_visible":{"menu_visible","menu_tampil"},
 "gameplay":{"gameplay","in_game","gameplay_ok","gameplay_berjalan"},
 "fps_median":{"fps_median","median_fps","fps","fps_rata2"},
 "stable_minutes":{"stable_minutes","durasi_menit","minutes","stability_minutes"},
 "crash":{"crash","crashed","crash_start","crash_saat_start"},
 "error":{"error","error_message","pesan_error"},
 "driver":{"driver","gpu_driver","driver_gpu"},
 "dxvk":{"dxvk"},
 "notes":{"notes","catatan","log","bukti"},
}
def norm(s): return re.sub(r"[^a-z0-9]+","_",s.strip().lower()).strip("_")
def truth(value):
 v=str(value or "").strip().lower()
 if v in {"1","true","yes","y","ya","ok","pass","terpasang","tampil","berjalan"}: return True
 if v in {"0","false","no","n","tidak","belum","fail","crash","gagal","tidak tampil"}: return False
 return None
def parse_markdown(text):
 rows=[]
 for line in text.splitlines():
  s=line.strip()
  if not s.startswith("|"): continue
  cells=[c.strip() for c in s.strip("|").split("|")]
  if cells and all(re.fullmatch(r":?-{3,}:?",c.replace(" ","")) for c in cells): continue
  rows.append(cells)
 if len(rows)<2: return []
 headers=[norm(x) for x in rows[0]]
 return [dict(zip(headers,cells)) for cells in rows[1:] if len(cells)==len(headers)]
def read_rows(path):
 text=path.read_text(encoding="utf-8-sig")
 if path.suffix.lower()==".csv": return list(csv.DictReader(text.splitlines()))
 if any(line.lstrip().startswith("|") for line in text.splitlines()): return parse_markdown(text)
 return list(csv.DictReader(text.splitlines()))
def canonical(row):
 source={norm(k):v for k,v in row.items() if k is not None}; result={}
 for key,aliases in ALIASES.items():
  for alias in aliases:
   if alias in source: result[key]=source[alias]; break
 return result
def classify(row):
 r=canonical(row); installed=truth(r.get("installed")); visible=truth(r.get("game_visible"))
 menu=truth(r.get("menu_visible")); gameplay=truth(r.get("gameplay")); crash=truth(r.get("crash"))
 try: fps=float(str(r.get("fps_median","")).replace(",","."))
 except (TypeError,ValueError): fps=None
 try: mins=float(str(r.get("stable_minutes","")).replace(",","."))
 except (TypeError,ValueError): mins=None
 if installed is False: return ("BLOCKER OS (usulan)","Winlator tidak terpasang/tidak dapat dijalankan; catat error dan identifikasi batas OS/sandbox sebelum memilih jalur.")
 if installed is True and (visible is False or crash is True): return ("MASALAH GRAFIS/KOMPONEN (usulan)","Game tidak tampil atau crash saat start; kumpulkan log dan ulangi dengan variasi driver GPU atau DXVK satu per satu.")
 if installed is True and (menu is True or visible is True) and fps is not None and fps<20: return ("TUNING PERFORMA (usulan)","Menu/frame tampil tetapi FPS di bawah 20; uji render scale dan preset satu variabel per sesi. Belum cukup untuk memilih arsitektur.")
 if installed is True and gameplay is True and fps is not None and fps>=30 and mins is not None and mins>=15 and crash is not True: return ("HYBRID PINNED LAYAK DIEVALUASI (usulan)","Gameplay mencapai ambang usulan. Catat konfigurasi terbukti sebagai kandidat hybrid; profil Ghost Town tetap untested sampai stack sendiri lolos.")
 return ("DATA BELUM CUKUP","Lengkapi status instalasi, visibilitas game, gameplay, FPS median, durasi stabil dan crash. Jangan putuskan arsitektur dari data parsial.")
def summarize(rows,source):
 lines=["# Ringkasan feasibility spike","",f"Sumber: {source}",f"Jumlah baris hasil: {len(rows)}",""]
 if not rows: lines += ["Tidak ada baris hasil yang dapat dibaca.","Rekomendasi: belum ada data; jangan memilih arsitektur."]; return "\n".join(lines)+"\n"
 for i,row in enumerate(rows,1):
  r=canonical(row); status,advice=classify(row); title=r.get("notes") or r.get("error") or f"Sesi {i}"
  lines += [f"## Hasil {i}: {title}",f"- Rekomendasi: **{status}**",f"- Tindak lanjut: {advice}"]
  for key in ("installed","game_visible","menu_visible","gameplay","fps_median","stable_minutes","crash","driver","dxvk","error"):
   if key in r and str(r[key]).strip(): lines.append(f"- {key}: {r[key]}")
  lines.append("")
 lines += ["## Keputusan arsitektur","Belum diputuskan otomatis. Terima dan tinjau data asli serta log pengguna terlebih dahulu. Jika hasil memenuhi matriks, itu hanya kelayakan kandidat; profil Ghost Town tetap untested sampai stack Ghost Town membuktikannya."]
 return "\n".join(lines)+"\n"
def main():
 p=argparse.ArgumentParser(description=__doc__); p.add_argument("input",type=pathlib.Path); p.add_argument("--output",type=pathlib.Path); a=p.parse_args()
 if not a.input.is_file(): p.error(f"file tidak ditemukan: {a.input}")
 report=summarize(read_rows(a.input),a.input.name)
 if a.output: a.output.write_text(report,encoding="utf-8")
 print(report,end="")
if __name__=="__main__": main()
