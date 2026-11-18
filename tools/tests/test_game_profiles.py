import importlib.util,json,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def load(name,path):
 s=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
v=load("validator",ROOT/"tools/validate_game_profile.py")
d=load("detector",ROOT/"tools/detect-unity-build.py")
class Tests(unittest.TestCase):
 def test_examples(self):
  for n in ("overcooked-2.json","moving-out-2.json"):
   x=json.loads((ROOT/"profiles"/n).read_text());self.assertEqual([],v.validate(x));self.assertEqual("untested",x["status"])
 def test_invalid(self):
  x=json.loads((ROOT/"profiles/overcooked-2.json").read_text());x["resolution"]["width"]=320;x["gpu"]["driver"]="bad";self.assertGreaterEqual(len(v.validate(x)),2)
 def test_mono_and_api(self):
  with tempfile.TemporaryDirectory() as t:
   p=Path(t);(p/"Game_Data/Managed").mkdir(parents=True);(p/"Game_Data/Managed/Assembly-CSharp.dll").touch();(p/"Player.log").write_text("Direct3D 11")
   x=d.detect(p);self.assertEqual("mono",x["unity_backend"]);self.assertIn("d3d11",x["graphics_apis"])
 def test_il2cpp(self):
  with tempfile.TemporaryDirectory() as t:
   p=Path(t);(p/"GameAssembly.dll").touch();self.assertEqual("il2cpp",d.detect(p)["unity_backend"])
if __name__=="__main__":unittest.main()
