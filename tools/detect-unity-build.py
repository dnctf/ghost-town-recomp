#!/usr/bin/env python3
import argparse,json
from pathlib import Path
def detect(folder):
 root=Path(folder); paths=list(root.rglob("*"))
 mono=any(p.as_posix().endswith("/Managed/Assembly-CSharp.dll") or p.name=="MonoBleedingEdge" for p in paths)
 il2cpp=any(p.name.lower()=="gameassembly.dll" for p in paths)
 backend="ambiguous" if mono and il2cpp else "mono" if mono else "il2cpp" if il2cpp else "unknown"
 apis=set(); evidence=[]
 if mono:evidence.append("Managed/Assembly-CSharp.dll or MonoBleedingEdge")
 if il2cpp:evidence.append("GameAssembly.dll")
 for p in paths:
  if p.is_file() and p.name.lower()=="player.log":
   try:
    for line in p.read_text(errors="ignore").splitlines():
     low=line.lower()
     if "direct3d 11" in low or "d3d11" in low:apis.add("d3d11")
     if "direct3d 12" in low or "d3d12" in low:apis.add("d3d12")
     if "vulkan" in low:apis.add("vulkan")
     if "opengl" in low:apis.add("opengl")
   except OSError:pass
   evidence.append("Player.log scanned for graphics API strings")
 return {"unity_backend":backend,"graphics_apis":sorted(apis) or ["unknown"],"evidence":evidence}
if __name__=="__main__":
 p=argparse.ArgumentParser();p.add_argument("game_folder");p.add_argument("--output",required=True);a=p.parse_args()
 result=detect(a.game_folder);Path(a.output).write_text(json.dumps(result,indent=2)+"\n");print(json.dumps(result,indent=2))
