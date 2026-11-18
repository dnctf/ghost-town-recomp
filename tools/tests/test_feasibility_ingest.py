import pathlib, tempfile, unittest
from tools.ingest_feasibility import classify, read_rows, summarize
class FeasibilityIngestTests(unittest.TestCase):
 def test_csv_os_blocker(self):
  self.assertEqual(classify({"installed":"no","error":"HarmonyOS menolak instalasi"})[0],"BLOCKER OS (usulan)")
 def test_markdown_graphics_failure(self):
  md="| installed | game_visible | crash | notes |\n|---|---|---|---|\n| yes | no | yes | blank saat start |"
  with tempfile.TemporaryDirectory() as td:
   p=pathlib.Path(td)/"result.md"; p.write_text(md); rows=read_rows(p)
  self.assertEqual(classify(rows[0])[0],"MASALAH GRAFIS/KOMPONEN (usulan)")
 def test_low_fps_tuning(self):
  self.assertEqual(classify({"installed":"yes","menu_visible":"yes","game_visible":"yes","fps_median":"19"})[0],"TUNING PERFORMA (usulan)")
 def test_stable_gameplay_hybrid_candidate(self):
  row={"installed":"yes","game_visible":"yes","gameplay":"yes","fps_median":"30","stable_minutes":"15","crash":"no","driver":"Turnip"}
  status,advice=classify(row); self.assertEqual(status,"HYBRID PINNED LAYAK DIEVALUASI (usulan)"); self.assertIn("tetap untested",advice)
 def test_insufficient_data_does_not_choose(self):
  self.assertEqual(classify({"installed":"yes"})[0],"DATA BELUM CUKUP")
 def test_summary_does_not_make_architecture_decision(self):
  text=summarize([{"installed":"yes","gameplay":"yes","fps_median":"30","stable_minutes":"15"}],"example.csv")
  self.assertIn("Belum diputuskan otomatis",text)
if __name__=="__main__": unittest.main()
