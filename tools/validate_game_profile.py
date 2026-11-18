#!/usr/bin/env python3
import json,re,sys
from pathlib import Path
def validate(d):
 e=[]
 if not isinstance(d,dict): return ["root must be object"]
 req={"schema_version","game_id","display_name","status","resolution","dll_overrides","environment","gpu","controls","unity_args","thermal","detection"}
 if req-set(d): e.append("missing required keys")
 if d.get("schema_version")!=1:e.append("schema_version must be 1")
 if not re.fullmatch(r"[a-z0-9][a-z0-9-]*",str(d.get("game_id",""))):e.append("invalid game_id")
 if d.get("status") not in {"untested","experimental","verified"}:e.append("invalid status")
 r=d.get("resolution",{})
 if not isinstance(r,dict) or not all(isinstance(r.get(k),(int,float)) and not isinstance(r.get(k),bool) for k in ("width","height","render_scale")) or not (640<=r["width"]<=3840 and 360<=r["height"]<=2160 and .5<=r["render_scale"]<=1):e.append("invalid resolution")
 if not isinstance(d.get("dll_overrides"),list):e.append("dll_overrides must be array")
 for x in d.get("dll_overrides",[]) if isinstance(d.get("dll_overrides"),list) else []:
  if not isinstance(x,dict) or not re.fullmatch(r"[A-Za-z0-9_.-]+",str(x.get("dll",""))) or x.get("mode") not in {"native","builtin","native,builtin","builtin,native","disabled"}:e.append("invalid DLL override")
 env=d.get("environment",{})
 if not isinstance(env,dict) or set(env)-{"box64","wine"}:e.append("invalid environment groups")
 else:
  for group in env.values():
   if not isinstance(group,dict) or any(v is not None and not isinstance(v,str) for v in group.values()):e.append("environment values must be strings or null")
 gpu=d.get("gpu",{})
 if not isinstance(gpu,dict) or gpu.get("driver") not in {"system","turnip","virgl","unknown"}:e.append("invalid GPU driver")
 c=d.get("controls",{})
 if not isinstance(c,dict) or c.get("preset") not in {"touch","xbox","keyboard","custom"}:e.append("invalid controls preset")
 if not isinstance(d.get("unity_args"),list) or any(not isinstance(x,str) for x in d["unity_args"]):e.append("unity_args must be string array")
 t=d.get("thermal",{})
 if not isinstance(t,dict) or t.get("preset") not in {"cool","balanced","performance"} or (t.get("fps_cap") is not None and (not isinstance(t["fps_cap"],int) or not 15<=t["fps_cap"]<=120)):e.append("invalid thermal preset")
 det=d.get("detection",{})
 if not isinstance(det,dict) or det.get("unity_backend") not in {"mono","il2cpp","ambiguous","unknown"} or not isinstance(det.get("graphics_apis"),list) or any(x not in {"d3d11","d3d12","vulkan","opengl","unknown"} for x in det["graphics_apis"]):e.append("invalid detection")
 return e
if __name__=="__main__":
 try:d=json.loads(Path(sys.argv[1]).read_text())
 except Exception as ex:print(ex,file=sys.stderr);raise SystemExit(2)
 errors=validate(d)
 if errors:print("\n".join(errors),file=sys.stderr);raise SystemExit(1)
 print("VALID: "+d["game_id"]+" ("+d["status"]+")")
